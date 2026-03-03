package com.robertafurucho.whatsapp.conversation;

import com.robertafurucho.order.CreateOrderRequest;
import com.robertafurucho.order.OrderResponse;
import com.robertafurucho.order.OrderService;
import com.robertafurucho.order.OrderStatus;
import com.robertafurucho.whatsapp.WhatsAppClient;
import com.robertafurucho.whatsapp.WhatsAppConfig;
import com.robertafurucho.whatsapp.message.WhatsAppMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ConversationService} — the chatbot state machine.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService")
class ConversationServiceTest {

    private static final String WA_ID = "5511999991234";
    private static final String MSG_ID = "wamid.abc123";

    @Mock private ConversationRepository repository;
    @Mock private StepValidator validator;
    @Mock private WhatsAppClient client;
    @Mock private OrderService orderService;

    private WhatsAppConfig config;
    private ConversationService service;

    @BeforeEach
    void setUp() {
        config = new WhatsAppConfig();
        config.setEnabled(true);
        service = new ConversationService(repository, validator, client, orderService, config);
    }

    /** Stubs repository.save() to return the argument as-is. */
    private void stubSave() {
        when(repository.save(any(ConversationState.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    /** Creates a state at a given step. */
    private ConversationState stateAt(ConversationStep step) {
        ConversationState s = new ConversationState();
        s.setWaId(WA_ID);
        s.setCurrentStep(step);
        return s;
    }

    /** Creates a fully-filled state ready for confirmation. */
    private ConversationState filledState() {
        ConversationState s = stateAt(ConversationStep.CONFIRM);
        s.setName("Roberta");
        s.setEmail("roberta@example.com");
        s.setPhone("11987654321");
        s.setAddress("Rua das Bonecas, 42");
        s.setPostalCode("01234-567");
        s.setOrderScope("Amigurumi");
        s.setOrderScopeDetail("Boneca rosa");
        s.setReceiveDate(LocalDate.now().plusMonths(2));
        return s;
    }

    // ── New conversation ──────────────────────────

    @Nested
    @DisplayName("new conversation (GREETING)")
    class Greeting {

        @Test
        @DisplayName("sends welcome message and advances to ASK_NAME")
        void sendsGreeting() {
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processMessage(WA_ID, MSG_ID, "text", "Oi", null);

            // Should send welcome text
            verify(client).sendText(eq(WA_ID), contains("Furucho"));
            // Should advance to ASK_NAME
            ArgumentCaptor<ConversationState> captor =
                ArgumentCaptor.forClass(ConversationState.class);
            verify(repository, atLeast(1)).save(captor.capture());
            ConversationState saved = captor.getAllValues().stream()
                .filter(s -> s.getCurrentStep() == ConversationStep.ASK_NAME)
                .findFirst().orElse(null);
            assertThat(saved).isNotNull();
        }
    }

    // ── Data collection ───────────────────────────

    @Nested
    @DisplayName("data collection (ASK_* steps)")
    class DataCollection {

        @Test
        @DisplayName("valid input advances to next step")
        void validInputAdvances() {
            stubSave();
            ConversationState state = stateAt(ConversationStep.ASK_NAME);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));
            when(validator.validate(ConversationStep.ASK_NAME, "Roberta"))
                .thenReturn(Optional.empty());

            service.processMessage(WA_ID, MSG_ID, "text", "Roberta", null);

            assertThat(state.getName()).isEqualTo("Roberta");
            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.ASK_EMAIL);
        }

        @Test
        @DisplayName("invalid input sends error and increments retry")
        void invalidInputRetries() {
            stubSave();
            ConversationState state = stateAt(ConversationStep.ASK_EMAIL);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));
            when(validator.validate(ConversationStep.ASK_EMAIL, "bad"))
                .thenReturn(Optional.of("Email inválido"));

            service.processMessage(WA_ID, MSG_ID, "text", "bad", null);

            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.ASK_EMAIL);
            assertThat(state.getRetryCount()).isEqualTo(1);
            verify(client).sendText(eq(WA_ID), contains("Email inválido"));
        }

        @Test
        @DisplayName("max retries reached auto-expires conversation")
        void maxRetriesAutoExpires() {
            stubSave();
            ConversationState state = stateAt(ConversationStep.ASK_EMAIL);
            state.setRetryCount(2); // one more will hit max (3)
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));
            when(validator.validate(ConversationStep.ASK_EMAIL, "bad"))
                .thenReturn(Optional.of("Email inválido"));

            service.processMessage(WA_ID, MSG_ID, "text", "bad", null);

            verify(client).sendText(eq(WA_ID), contains("máximo de tentativas"));
            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.EXPIRED);
            assertThat(state.isExpired()).isTrue();
        }
    }

    // ── Order scope (interactive buttons) ─────────

    @Nested
    @DisplayName("ASK_ORDER_SCOPE (interactive buttons)")
    class OrderScope {

        @Test
        @DisplayName("accepts button reply and advances to ASK_DETAIL")
        void buttonReplyAdvances() {
            stubSave();
            ConversationState state = stateAt(ConversationStep.ASK_ORDER_SCOPE);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));
            when(validator.validate(eq(ConversationStep.ASK_ORDER_SCOPE), eq("Amigurumi")))
                .thenReturn(Optional.empty());

            WhatsAppMessage.Interactive interactive = new WhatsAppMessage.Interactive(
                "button_reply",
                new WhatsAppMessage.ButtonReply("tipo_amigurumi", "Amigurumi"),
                null
            );

            service.processMessage(WA_ID, MSG_ID, "interactive", null, interactive);

            assertThat(state.getOrderScope()).isEqualTo("Amigurumi");
            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.ASK_DETAIL);
        }

        @Test
        @DisplayName("accepts free-text for custom type")
        void freeTextAccepted() {
            stubSave();
            ConversationState state = stateAt(ConversationStep.ASK_ORDER_SCOPE);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));
            when(validator.validate(eq(ConversationStep.ASK_ORDER_SCOPE), eq("Boneca de Pano")))
                .thenReturn(Optional.empty());

            service.processMessage(WA_ID, MSG_ID, "text", "Boneca de Pano", null);

            assertThat(state.getOrderScope()).isEqualTo("Boneca de Pano");
        }
    }

    // ── Confirmation ──────────────────────────────

    @Nested
    @DisplayName("CONFIRM step")
    class Confirm {

        @Test
        @DisplayName("confirm_yes creates order and completes conversation")
        void confirmCreatesOrder() {
            stubSave();
            ConversationState state = filledState();
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));

            OrderResponse response = new OrderResponse(
                42L, "Roberta", "roberta@example.com", "11987654321",
                "Rua das Bonecas, 42", "01234567", "Amigurumi", "Boneca rosa",
                LocalDate.now().plusMonths(2), LocalDateTime.now(), OrderStatus.PENDING
            );
            when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

            WhatsAppMessage.Interactive interactive = new WhatsAppMessage.Interactive(
                "button_reply",
                new WhatsAppMessage.ButtonReply("confirm_yes", "✓ Confirmar"),
                null
            );

            service.processMessage(WA_ID, MSG_ID, "interactive", null, interactive);

            // Verify field mapping via ArgumentCaptor
            ArgumentCaptor<CreateOrderRequest> reqCaptor =
                ArgumentCaptor.forClass(CreateOrderRequest.class);
            verify(orderService).createOrder(reqCaptor.capture());
            CreateOrderRequest captured = reqCaptor.getValue();
            assertThat(captured.name()).isEqualTo("Roberta");
            assertThat(captured.email()).isEqualTo("roberta@example.com");
            assertThat(captured.phone()).isEqualTo("11987654321");
            assertThat(captured.address()).isEqualTo("Rua das Bonecas, 42");
            assertThat(captured.postalCode()).isEqualTo("01234-567");
            assertThat(captured.orderScope()).isEqualTo("Amigurumi");
            assertThat(captured.orderScopeDetail()).isEqualTo("Boneca rosa");

            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.COMPLETED);
            verify(client).sendText(eq(WA_ID), contains("#42"));
        }

        @Test
        @DisplayName("confirm_edit moves to CORRECTION step")
        void confirmEditGoesToCorrection() {
            stubSave();
            ConversationState state = filledState();
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));

            WhatsAppMessage.Interactive interactive = new WhatsAppMessage.Interactive(
                "button_reply",
                new WhatsAppMessage.ButtonReply("confirm_edit", "✏ Corrigir"),
                null
            );

            service.processMessage(WA_ID, MSG_ID, "interactive", null, interactive);

            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.CORRECTION);
            verify(client).sendInteractive(eq(WA_ID), anyMap());
        }

        @Test
        @DisplayName("confirm_cancel expires conversation")
        void confirmCancelExpires() {
            stubSave();
            ConversationState state = filledState();
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));

            WhatsAppMessage.Interactive interactive = new WhatsAppMessage.Interactive(
                "button_reply",
                new WhatsAppMessage.ButtonReply("confirm_cancel", "✗ Cancelar"),
                null
            );

            service.processMessage(WA_ID, MSG_ID, "interactive", null, interactive);

            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.EXPIRED);
            assertThat(state.isExpired()).isTrue();
        }

        @Test
        @DisplayName("order creation failure expires conversation and notifies user")
        void orderCreationFailure() {
            stubSave();
            ConversationState state = filledState();
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));
            when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

            WhatsAppMessage.Interactive interactive = new WhatsAppMessage.Interactive(
                "button_reply",
                new WhatsAppMessage.ButtonReply("confirm_yes", "✓ Confirmar"),
                null
            );

            service.processMessage(WA_ID, MSG_ID, "interactive", null, interactive);

            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.EXPIRED);
            assertThat(state.isExpired()).isTrue();
            verify(client).sendText(eq(WA_ID), contains("erro"));
        }
    }

    // ── Correction ────────────────────────────────

    @Nested
    @DisplayName("CORRECTION step")
    class Correction {

        @Test
        @DisplayName("list reply navigates to the selected field step with correcting flag")
        void listReplyNavigates() {
            stubSave();
            ConversationState state = filledState();
            state.setCurrentStep(ConversationStep.CORRECTION);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));

            WhatsAppMessage.Interactive interactive = new WhatsAppMessage.Interactive(
                "list_reply",
                null,
                new WhatsAppMessage.ListReply("fix_name", "Nome", null)
            );

            service.processMessage(WA_ID, MSG_ID, "interactive", null, interactive);

            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.ASK_NAME);
            assertThat(state.isCorrecting()).isTrue();
            verify(client).sendText(eq(WA_ID), contains("Valor atual"));
        }

        @Test
        @DisplayName("corrected field returns to CONFIRM instead of continuing sequential flow")
        void correctionReturnsToConfirm() {
            stubSave();
            ConversationState state = filledState();
            state.setCurrentStep(ConversationStep.ASK_NAME);
            state.setCorrecting(true);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));
            when(validator.validate(ConversationStep.ASK_NAME, "Roberta Atualizada"))
                .thenReturn(Optional.empty());

            service.processMessage(WA_ID, "msg2", "text", "Roberta Atualizada", null);

            assertThat(state.getName()).isEqualTo("Roberta Atualizada");
            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.CONFIRM);
            assertThat(state.isCorrecting()).isFalse();
            verify(client).sendInteractive(eq(WA_ID), anyMap());
        }
    }

    // ── Special commands ──────────────────────────

    @Nested
    @DisplayName("special commands")
    class SpecialCommands {

        @Test
        @DisplayName("/cancelar expires the conversation")
        void cancelCommand() {
            stubSave();
            ConversationState state = stateAt(ConversationStep.ASK_EMAIL);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));

            service.processMessage(WA_ID, MSG_ID, "text", "/cancelar", null);

            assertThat(state.getCurrentStep()).isEqualTo(ConversationStep.EXPIRED);
            verify(client).sendText(eq(WA_ID), contains("cancelada"));
        }

        @Test
        @DisplayName("/status shows last completed order without creating orphan conversation")
        void statusCommand() {
            ConversationState completed = filledState();
            completed.setCurrentStep(ConversationStep.COMPLETED);
            completed.setCompletedAt(LocalDateTime.now());
            when(repository.findLastCompletedByWaId(WA_ID))
                .thenReturn(Optional.of(completed));

            service.processMessage(WA_ID, MSG_ID, "text", "/status", null);

            verify(client).sendText(eq(WA_ID), contains("Último pedido"));
            // Should NOT have created any conversation
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("/ajuda shows help text without creating orphan conversation")
        void helpCommand() {
            service.processMessage(WA_ID, MSG_ID, "text", "/ajuda", null);

            verify(client).sendText(eq(WA_ID), contains("Comandos disponíveis"));
            // Should NOT have created any conversation
            verify(repository, never()).save(any());
        }
    }

    // ── Edge cases ────────────────────────────────

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("duplicate message ID is ignored")
        void duplicateMessageSkipped() {
            ConversationState state = stateAt(ConversationStep.ASK_NAME);
            state.setLastMessageId(MSG_ID);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));

            service.processMessage(WA_ID, MSG_ID, "text", "Hello", null);

            verify(client, never()).sendText(anyString(), anyString());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("non-text/interactive messages are rejected")
        void nonTextRejected() {
            stubSave();
            ConversationState state = stateAt(ConversationStep.ASK_NAME);
            when(repository.findActiveByWaId(WA_ID)).thenReturn(Optional.of(state));

            service.processMessage(WA_ID, MSG_ID, "image", null, null);

            verify(client).sendText(eq(WA_ID), contains("texto"));
        }
    }
}
