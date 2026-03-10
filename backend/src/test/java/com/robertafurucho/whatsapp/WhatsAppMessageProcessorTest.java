package com.robertafurucho.whatsapp;

import com.robertafurucho.whatsapp.conversation.ConversationService;
import com.robertafurucho.whatsapp.message.WhatsAppMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WhatsAppMessageProcessor}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WhatsAppMessageProcessor")
class WhatsAppMessageProcessorTest {

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private WhatsAppMessageProcessor processor;

    @Test
    @DisplayName("delegates text message to ConversationService")
    void delegatesTextMessage() {
        processor.processMessage("5511999991234", "msg1", "text", "Oi", null);

        verify(conversationService).processMessage(
            "5511999991234", "msg1", "text", "Oi", null);
    }

    @Test
    @DisplayName("delegates interactive message to ConversationService")
    void delegatesInteractiveMessage() {
        var interactive = new WhatsAppMessage.Interactive(
            "button_reply",
            new WhatsAppMessage.ButtonReply("confirm_yes", "Confirmar"),
            null
        );

        processor.processMessage("5511999991234", "msg2", "interactive", null, interactive);

        verify(conversationService).processMessage(
            "5511999991234", "msg2", "interactive", null, interactive);
    }

    @Test
    @DisplayName("catches and logs exceptions without propagating")
    void catchesExceptions() {
        doThrow(new RuntimeException("DB error"))
            .when(conversationService).processMessage(anyString(), anyString(),
                anyString(), anyString(), any());

        // Should NOT throw — error is caught and logged
        processor.processMessage("5511999991234", "msg3", "text", "Oi", null);

        verify(conversationService).processMessage(
            "5511999991234", "msg3", "text", "Oi", null);
    }
}
