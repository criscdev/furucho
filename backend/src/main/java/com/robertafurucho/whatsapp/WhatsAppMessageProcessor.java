package com.robertafurucho.whatsapp;

import com.robertafurucho.whatsapp.conversation.ConversationService;
import com.robertafurucho.whatsapp.message.WhatsAppMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Processes WhatsApp webhook messages asynchronously.
 *
 * <p>Decouples the HTTP webhook handler from message processing to ensure
 * the POST response returns within Meta's 5-second window. Each message
 * is processed on a dedicated thread pool ({@code whatsappExecutor}).
 */
@Component
public class WhatsAppMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageProcessor.class);

    private final ConversationService conversationService;

    public WhatsAppMessageProcessor(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * Processes a single inbound WhatsApp message asynchronously.
     *
     * @param waId        WhatsApp sender ID
     * @param messageId   unique message ID (for idempotency)
     * @param type        message type ("text", "interactive", etc.)
     * @param textBody    text body (null for non-text messages)
     * @param interactive interactive payload (null for non-interactive messages)
     */
    @Async("whatsappExecutor")
    public void processMessage(
            String waId,
            String messageId,
            String type,
            String textBody,
            WhatsAppMessage.Interactive interactive) {
        try {
            conversationService.processMessage(waId, messageId, type, textBody, interactive);
        } catch (Exception e) {
            log.error("Error processing message {} from {}: {}",
                messageId, waId, e.getMessage(), e);
        }
    }
}
