package com.robertafurucho.whatsapp.scheduler;

import com.robertafurucho.whatsapp.WhatsAppClient;
import com.robertafurucho.whatsapp.WhatsAppConfig;
import com.robertafurucho.whatsapp.conversation.ConversationRepository;
import com.robertafurucho.whatsapp.conversation.ConversationState;
import com.robertafurucho.whatsapp.conversation.ConversationStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that expires idle WhatsApp conversations.
 *
 * <p>Runs every 60 seconds. Conversations with no activity for longer than
 * {@code whatsapp.conversation.timeout-minutes} (default 30) are marked
 * as EXPIRED and the customer receives a farewell message.
 */
@Component
public class ConversationExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ConversationExpirationJob.class);

    private final ConversationRepository repository;
    private final WhatsAppClient client;
    private final WhatsAppConfig config;

    public ConversationExpirationJob(
            ConversationRepository repository,
            WhatsAppClient client,
            WhatsAppConfig config) {
        this.repository = repository;
        this.client = client;
        this.config = config;
    }

    /**
     * Scans for timed-out conversations and expires them.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireIdleConversations() {
        if (!config.isEnabled()) {
            return;
        }

        int timeoutMinutes = config.getConversation().getTimeoutMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ConversationState> expired = repository.findExpiredConversations(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Expiring {} idle conversation(s) (timeout={}min)", expired.size(), timeoutMinutes);

        for (ConversationState state : expired) {
            state.setCurrentStep(ConversationStep.EXPIRED);
            state.setExpired(true);

            client.sendText(state.getWaId(),
                "⏰ Sua conversa expirou por inatividade.\n" +
                "Envie qualquer mensagem para começar uma nova encomenda! 🧸");

            log.debug("Expired conversation id={} waId={}", state.getId(), state.getWaId());
        }

        repository.saveAll(expired);
    }
}
