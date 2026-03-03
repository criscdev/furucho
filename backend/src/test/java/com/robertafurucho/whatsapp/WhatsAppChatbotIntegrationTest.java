package com.robertafurucho.whatsapp;

import com.robertafurucho.order.OrderRepository;
import com.robertafurucho.whatsapp.conversation.ConversationRepository;
import com.robertafurucho.whatsapp.conversation.ConversationService;
import com.robertafurucho.whatsapp.conversation.ConversationState;
import com.robertafurucho.whatsapp.conversation.ConversationStep;
import com.robertafurucho.whatsapp.message.WhatsAppMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration test for the full WhatsApp chatbot conversation flow.
 *
 * <p>Uses {@code @SpringBootTest} with a real H2 database and full Spring context.
 * The {@link WhatsAppClient} is mocked to prevent real HTTP calls.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WhatsApp Chatbot Integration (full flow)")
class WhatsAppChatbotIntegrationTest {

    private static final String WA_ID = "5511999991234";
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(3);
    private static final String FUTURE_DATE_STR =
        FUTURE_DATE.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private WhatsAppClient whatsAppClient;

    @BeforeEach
    void cleanDatabase() {
        conversationRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("complete order flow: greeting → 8 fields → confirm → order created")
    void completeOrderFlow() {
        long initialOrders = orderRepository.count();

        // 1. First message → triggers GREETING → ASK_NAME
        conversationService.processMessage(WA_ID, "msg1", "text", "Oi", null);
        verify(whatsAppClient, atLeastOnce()).sendText(eq(WA_ID), contains("Furucho"));

        // 2. Name
        conversationService.processMessage(WA_ID, "msg2", "text", "Roberta Furucho", null);
        assertCurrentStep(ConversationStep.ASK_EMAIL);

        // 3. Email
        conversationService.processMessage(WA_ID, "msg3", "text", "roberta@example.com", null);
        assertCurrentStep(ConversationStep.ASK_PHONE);

        // 4. Phone
        conversationService.processMessage(WA_ID, "msg4", "text", "11987654321", null);
        assertCurrentStep(ConversationStep.ASK_ADDRESS);

        // 5. Address
        conversationService.processMessage(WA_ID, "msg5", "text", "Rua das Bonecas, 42", null);
        assertCurrentStep(ConversationStep.ASK_POSTAL_CODE);

        // 6. CEP
        conversationService.processMessage(WA_ID, "msg6", "text", "01234-567", null);
        assertCurrentStep(ConversationStep.ASK_ORDER_SCOPE);

        // 7. Order scope (interactive button)
        WhatsAppMessage.Interactive scopeButton = new WhatsAppMessage.Interactive(
            "button_reply",
            new WhatsAppMessage.ButtonReply("tipo_amigurumi", "Amigurumi"),
            null
        );
        conversationService.processMessage(WA_ID, "msg7", "interactive", null, scopeButton);
        assertCurrentStep(ConversationStep.ASK_DETAIL);

        // 8. Detail
        conversationService.processMessage(WA_ID, "msg8", "text",
            "Boneca rosa com vestido azul", null);
        assertCurrentStep(ConversationStep.ASK_DATE);

        // 9. Date
        conversationService.processMessage(WA_ID, "msg9", "text", FUTURE_DATE_STR, null);
        assertCurrentStep(ConversationStep.CONFIRM);

        // 10. Confirm
        WhatsAppMessage.Interactive confirmButton = new WhatsAppMessage.Interactive(
            "button_reply",
            new WhatsAppMessage.ButtonReply("confirm_yes", "✓ Confirmar"),
            null
        );
        conversationService.processMessage(WA_ID, "msg10", "interactive", null, confirmButton);

        // Verify order was created
        assertThat(orderRepository.count()).isEqualTo(initialOrders + 1);
        assertCurrentStep(ConversationStep.COMPLETED);
        verify(whatsAppClient).sendText(eq(WA_ID), contains("sucesso"));
    }

    @Test
    @DisplayName("correction flow: fill all → edit name → returns to confirm → order created")
    void correctionFlow() {
        // Fill all fields to CONFIRM
        fillAllFields();
        assertCurrentStep(ConversationStep.CONFIRM);

        // Choose to edit
        WhatsAppMessage.Interactive editButton = new WhatsAppMessage.Interactive(
            "button_reply",
            new WhatsAppMessage.ButtonReply("confirm_edit", "✏ Corrigir"),
            null
        );
        conversationService.processMessage(WA_ID, "msg_edit", "interactive", null, editButton);
        assertCurrentStep(ConversationStep.CORRECTION);

        // Select name field to correct
        WhatsAppMessage.Interactive fixName = new WhatsAppMessage.Interactive(
            "list_reply",
            null,
            new WhatsAppMessage.ListReply("fix_name", "Nome", null)
        );
        conversationService.processMessage(WA_ID, "msg_fix", "interactive", null, fixName);
        assertCurrentStep(ConversationStep.ASK_NAME);

        // Provide new name → should return directly to CONFIRM (not sequential flow)
        conversationService.processMessage(WA_ID, "msg_newname", "text",
            "Roberta Furucho Atualizada", null);
        assertCurrentStep(ConversationStep.CONFIRM);

        // Verify the corrected name is stored
        ConversationState state = conversationRepository.findActiveByWaId(WA_ID).orElseThrow();
        assertThat(state.getName()).isEqualTo("Roberta Furucho Atualizada");

        // Now confirm
        WhatsAppMessage.Interactive confirmButton = new WhatsAppMessage.Interactive(
            "button_reply",
            new WhatsAppMessage.ButtonReply("confirm_yes", "✓ Confirmar"),
            null
        );
        conversationService.processMessage(WA_ID, "msg_final_confirm", "interactive",
            null, confirmButton);

        assertThat(orderRepository.count()).isEqualTo(1);
        assertCurrentStep(ConversationStep.COMPLETED);
    }

    @Test
    @DisplayName("cancel command resets conversation")
    void cancelResets() {
        conversationService.processMessage(WA_ID, "msg1", "text", "Oi", null);
        conversationService.processMessage(WA_ID, "msg2", "text", "Roberta", null);
        assertCurrentStep(ConversationStep.ASK_EMAIL);

        // Cancel
        conversationService.processMessage(WA_ID, "msg3", "text", "/cancelar", null);
        assertCurrentStep(ConversationStep.EXPIRED);

        // New message starts new conversation
        conversationService.processMessage(WA_ID, "msg4", "text", "Oi de novo", null);
        // Should have a new GREETING → ASK_NAME state
        Optional<ConversationState> active = conversationRepository.findActiveByWaId(WA_ID);
        assertThat(active).isPresent();
        assertThat(active.get().getCurrentStep()).isEqualTo(ConversationStep.ASK_NAME);
    }

    @Test
    @DisplayName("/status and /ajuda do NOT create orphan conversations")
    void statelessCommandsNoOrphans() {
        // /status without any existing conversation
        conversationService.processMessage(WA_ID, "s1", "text", "/status", null);
        verify(whatsAppClient).sendText(eq(WA_ID), contains("Nenhum pedido"));
        assertThat(conversationRepository.count()).isZero();

        // /ajuda without any existing conversation
        conversationService.processMessage(WA_ID, "s2", "text", "/ajuda", null);
        verify(whatsAppClient).sendText(eq(WA_ID), contains("Comandos disponíveis"));
        assertThat(conversationRepository.count()).isZero();
    }

    // ── Helpers ───────────────────────────────────

    private void assertCurrentStep(ConversationStep expected) {
        Optional<ConversationState> state = conversationRepository.findActiveByWaId(WA_ID);
        if (expected == ConversationStep.COMPLETED || expected == ConversationStep.EXPIRED) {
            // Terminal states: conversation no longer "active"
            // Look for the most recent by waId
            var all = conversationRepository.findAll().stream()
                .filter(s -> s.getWaId().equals(WA_ID))
                .toList();
            assertThat(all).isNotEmpty();
            ConversationState last = all.getLast();
            assertThat(last.getCurrentStep()).isEqualTo(expected);
        } else {
            assertThat(state).isPresent();
            assertThat(state.get().getCurrentStep()).isEqualTo(expected);
        }
    }

    private void fillAllFields() {
        conversationService.processMessage(WA_ID, "f1", "text", "Oi", null);
        conversationService.processMessage(WA_ID, "f2", "text", "Roberta", null);
        fillFromStep(ConversationStep.ASK_EMAIL);
    }

    private void fillFromStep(ConversationStep fromStep) {
        int msgCounter = 100;
        if (fromStep.ordinal() <= ConversationStep.ASK_EMAIL.ordinal()) {
            conversationService.processMessage(WA_ID, "f" + (++msgCounter), "text",
                "roberta@example.com", null);
        }
        if (fromStep.ordinal() <= ConversationStep.ASK_PHONE.ordinal()) {
            conversationService.processMessage(WA_ID, "f" + (++msgCounter), "text",
                "11987654321", null);
        }
        if (fromStep.ordinal() <= ConversationStep.ASK_ADDRESS.ordinal()) {
            conversationService.processMessage(WA_ID, "f" + (++msgCounter), "text",
                "Rua das Bonecas, 42", null);
        }
        if (fromStep.ordinal() <= ConversationStep.ASK_POSTAL_CODE.ordinal()) {
            conversationService.processMessage(WA_ID, "f" + (++msgCounter), "text",
                "01234-567", null);
        }
        if (fromStep.ordinal() <= ConversationStep.ASK_ORDER_SCOPE.ordinal()) {
            WhatsAppMessage.Interactive scopeButton = new WhatsAppMessage.Interactive(
                "button_reply",
                new WhatsAppMessage.ButtonReply("tipo_amigurumi", "Amigurumi"),
                null
            );
            conversationService.processMessage(WA_ID, "f" + (++msgCounter), "interactive",
                null, scopeButton);
        }
        if (fromStep.ordinal() <= ConversationStep.ASK_DETAIL.ordinal()) {
            conversationService.processMessage(WA_ID, "f" + (++msgCounter), "text",
                "Boneca rosa com vestido azul", null);
        }
        if (fromStep.ordinal() <= ConversationStep.ASK_DATE.ordinal()) {
            conversationService.processMessage(WA_ID, "f" + (++msgCounter), "text",
                FUTURE_DATE_STR, null);
        }
    }
}
