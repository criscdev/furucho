package com.robertafurucho.whatsapp.conversation;

import com.robertafurucho.order.CreateOrderRequest;
import com.robertafurucho.order.OrderResponse;
import com.robertafurucho.order.OrderService;
import com.robertafurucho.whatsapp.WhatsAppClient;
import com.robertafurucho.whatsapp.WhatsAppConfig;
import com.robertafurucho.whatsapp.message.InteractiveMessageBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Core state-machine orchestrating the WhatsApp order chatbot.
 *
 * <p>Each inbound message is processed step-by-step:
 * <ol>
 *   <li>Find or create an active {@link ConversationState} for the sender</li>
 *   <li>Handle special commands ({@code /cancelar}, {@code /status}, {@code /ajuda})</li>
 *   <li>Reject non-text/interactive messages</li>
 *   <li>Validate input, store field, advance step, send next prompt</li>
 *   <li>On CONFIRM: display summary with confirm/edit/cancel buttons</li>
 *   <li>On confirm → create order via {@link OrderService}, complete conversation</li>
 * </ol>
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository repository;
    private final StepValidator validator;
    private final WhatsAppClient client;
    private final OrderService orderService;
    private final WhatsAppConfig config;

    public ConversationService(
            ConversationRepository repository,
            StepValidator validator,
            WhatsAppClient client,
            OrderService orderService,
            WhatsAppConfig config) {
        this.repository = repository;
        this.validator = validator;
        this.client = client;
        this.orderService = orderService;
        this.config = config;
    }

    /**
     * Processes a single inbound WhatsApp message.
     *
     * @param waId        the sender's WhatsApp ID (e.g. "5511999991234")
     * @param messageId   the unique message ID (for idempotency)
     * @param type        the message type ("text", "interactive", "image", etc.)
     * @param textBody    the text body (if type == "text"), may be null
     * @param interactive the interactive reply payload (if type == "interactive"), may be null
     */
    @Transactional
    public void processMessage(String waId, String messageId, String type,
                               String textBody,
                               com.robertafurucho.whatsapp.message.WhatsAppMessage.Interactive interactive) {
        log.info("Processing message from {} [type={}, id={}]", waId, type, messageId);

        // 0. Handle stateless commands BEFORE creating a conversation (prevents orphans)
        if ("text".equals(type) && textBody != null) {
            String trimmed = textBody.trim().toLowerCase();
            if (trimmed.equals("/status")) {
                handleStatus(waId);
                return;
            }
            if (trimmed.equals("/ajuda")) {
                handleHelp(waId);
                return;
            }
        }

        // 1. Find or create conversation
        ConversationState state = repository.findActiveByWaId(waId)
            .orElseGet(() -> createNewConversation(waId));

        // 2. Idempotency: skip if we already processed this message
        if (messageId != null && messageId.equals(state.getLastMessageId())) {
            log.debug("Duplicate message {} — skipping", messageId);
            return;
        }
        state.setLastMessageId(messageId);

        // 3. Handle /cancelar command (needs the state to mark as expired)
        if ("text".equals(type) && textBody != null) {
            String trimmed = textBody.trim().toLowerCase();
            if (trimmed.equals("/cancelar")) {
                handleCancel(state);
                return;
            }
        }

        // 4. Reject non-text/interactive messages
        if (!"text".equals(type) && !"interactive".equals(type)) {
            client.sendText(waId,
                "📝 Por favor, envie sua resposta como *texto*.\n" +
                "Não consigo processar imagens, áudios ou outros tipos de mídia.");
            repository.save(state);
            return;
        }

        // 5. Route by current step
        ConversationStep currentStep = state.getCurrentStep();
        switch (currentStep) {
            case GREETING -> handleGreeting(state);
            case ASK_ORDER_SCOPE -> handleOrderScope(state, type, textBody, interactive);
            case CONFIRM -> handleConfirm(state, type, textBody, interactive);
            case CORRECTION -> handleCorrection(state, type, interactive);
            case ASK_NAME, ASK_EMAIL, ASK_PHONE, ASK_ADDRESS,
                 ASK_POSTAL_CODE, ASK_DETAIL, ASK_DATE
                 -> handleDataCollection(state, textBody);
            case COMPLETED, EXPIRED -> {
                // Terminal states — start a new conversation
                ConversationState newState = createNewConversation(waId);
                handleGreeting(newState);
            }
        }
    }

    // ---- Step handlers ----

    private void handleGreeting(ConversationState state) {
        client.sendText(state.getWaId(),
            "🧸 Olá! Bem-vindo(a) à *Furucho Bonecas Artesanais*! 🎀\n\n" +
            "Vou ajudar você a fazer uma encomenda personalizada.\n" +
            "Para começar, qual é o seu *nome completo*?\n\n" +
            "💡 _Dica: a qualquer momento, envie /cancelar para recomeçar, " +
            "/status para consultar seu último pedido ou /ajuda para ver os comandos._");
        state.setCurrentStep(ConversationStep.ASK_NAME);
        repository.save(state);
    }

    private void handleDataCollection(ConversationState state, String textBody) {
        ConversationStep step = state.getCurrentStep();
        String waId = state.getWaId();
        String input = textBody != null ? textBody.trim() : "";

        // Validate
        Optional<String> error = validator.validate(step, input);
        if (error.isPresent()) {
            int retries = state.getRetryCount() + 1;
            int maxRetries = config.getConversation().getMaxRetriesPerField();

            if (retries >= maxRetries) {
                client.sendText(waId,
                    "❌ " + error.get() + "\n\n" +
                    "Número máximo de tentativas atingido. " +
                    "Envie qualquer mensagem para começar novamente.");
                state.setCurrentStep(ConversationStep.EXPIRED);
                state.setExpired(true);
                state.setCorrecting(false);
                repository.save(state);
                return;
            } else {
                client.sendText(waId,
                    "⚠️ " + error.get() + "\n" +
                    "Por favor, tente novamente:");
            }
            state.setRetryCount(retries);
            repository.save(state);
            return;
        }

        // Store valid value and advance
        state.setFieldForStep(step, input);
        state.setRetryCount(0);

        if (state.isCorrecting()) {
            // Correction complete — return to confirmation
            state.setCorrecting(false);
            state.setCurrentStep(ConversationStep.CONFIRM);
            repository.save(state);
            resendConfirmation(state);
        } else {
            ConversationStep nextStep = step.next();
            state.setCurrentStep(nextStep);
            repository.save(state);
            sendPromptForStep(state, nextStep);
        }
    }

    private void handleOrderScope(ConversationState state, String type,
                                  String textBody,
                                  com.robertafurucho.whatsapp.message.WhatsAppMessage.Interactive interactive) {
        String waId = state.getWaId();
        String input = null;

        // Accept interactive button reply
        if ("interactive".equals(type) && interactive != null
                && interactive.buttonReply() != null) {
            String buttonId = interactive.buttonReply().id();
            input = switch (buttonId) {
                case "tipo_amigurumi" -> "Amigurumi";
                case "tipo_pano" -> "Boneca de Pano";
                case "tipo_outro" -> "Outro tipo";
                default -> null;
            };
        }

        // Also accept free-text for "Outro tipo"
        if (input == null && "text".equals(type) && textBody != null) {
            input = textBody.trim();
        }

        if (input == null || input.isBlank()) {
            client.sendInteractive(waId, InteractiveMessageBuilder.orderScopeButtons());
            repository.save(state);
            return;
        }

        // Validate and store
        Optional<String> error = validator.validate(ConversationStep.ASK_ORDER_SCOPE, input);
        if (error.isPresent()) {
            client.sendText(waId, "⚠️ " + error.get());
            client.sendInteractive(waId, InteractiveMessageBuilder.orderScopeButtons());
            repository.save(state);
            return;
        }

        state.setFieldForStep(ConversationStep.ASK_ORDER_SCOPE, input);
        state.setRetryCount(0);

        if (state.isCorrecting()) {
            // Correction complete — return to confirmation
            state.setCorrecting(false);
            state.setCurrentStep(ConversationStep.CONFIRM);
            repository.save(state);
            resendConfirmation(state);
        } else {
            state.setCurrentStep(ConversationStep.ASK_DETAIL);
            repository.save(state);
            sendPromptForStep(state, ConversationStep.ASK_DETAIL);
        }
    }

    private void handleConfirm(ConversationState state, String type,
                               String textBody,
                               com.robertafurucho.whatsapp.message.WhatsAppMessage.Interactive interactive) {
        String waId = state.getWaId();

        // Expect interactive button reply
        if ("interactive".equals(type) && interactive != null
                && interactive.buttonReply() != null) {
            String buttonId = interactive.buttonReply().id();

            switch (buttonId) {
                case "confirm_yes" -> confirmOrder(state);
                case "confirm_edit" -> {
                    state.setCurrentStep(ConversationStep.CORRECTION);
                    repository.save(state);
                    client.sendInteractive(waId,
                        InteractiveMessageBuilder.correctionList(state));
                }
                case "confirm_cancel" -> handleCancel(state);
                default -> resendConfirmation(state);
            }
            return;
        }

        // Text /cancelar already handled above; anything else → re-send
        resendConfirmation(state);
    }

    private void handleCorrection(ConversationState state, String type,
                                  com.robertafurucho.whatsapp.message.WhatsAppMessage.Interactive interactive) {
        String waId = state.getWaId();

        // Expect interactive list reply (field selection)
        if ("interactive".equals(type) && interactive != null
                && interactive.listReply() != null) {
            String fieldId = interactive.listReply().id();
            try {
                ConversationStep targetStep = ConversationStep.fromCorrectionFieldId(fieldId);
                state.setCurrentStep(targetStep);
                state.setCorrecting(true);
                state.setRetryCount(0);
                repository.save(state);

                // Show current value and ask for new one
                String currentValue = state.getFieldValueForStep(targetStep);
                String prompt = getPromptText(targetStep);
                client.sendText(waId,
                    "✏️ Valor atual: *" + (currentValue != null ? currentValue : "—") + "*\n\n" +
                    prompt);

                // For ASK_ORDER_SCOPE, also send buttons
                if (targetStep == ConversationStep.ASK_ORDER_SCOPE) {
                    client.sendInteractive(waId,
                        InteractiveMessageBuilder.orderScopeButtons());
                }
                return;
            } catch (IllegalArgumentException e) {
                log.warn("Unknown correction field: {}", fieldId);
            }
        }

        // Unrecognized — re-send the correction list
        client.sendInteractive(waId,
            InteractiveMessageBuilder.correctionList(state));
        repository.save(state);
    }

    // ---- Order finalization ----

    private void confirmOrder(ConversationState state) {
        String waId = state.getWaId();
        try {
            CreateOrderRequest request = new CreateOrderRequest(
                state.getName(),
                state.getEmail(),
                state.getPhone(),
                state.getAddress(),
                state.getPostalCode(),
                state.getOrderScope(),
                state.getOrderScopeDetail(),
                state.getReceiveDate()
            );

            OrderResponse order = orderService.createOrder(request);

            state.setCurrentStep(ConversationStep.COMPLETED);
            state.setCompletedAt(LocalDateTime.now());
            repository.save(state);

            client.sendText(waId,
                "✅ *Pedido #" + order.id() + " criado com sucesso!*\n\n" +
                "Você receberá atualizações sobre o andamento.\n" +
                "Para consultar o status, envie /status a qualquer momento.\n\n" +
                "Obrigada pela preferência! 🧸💕");

            log.info("Order #{} created from WhatsApp conversation waId={}", order.id(), waId);

        } catch (Exception e) {
            log.error("Failed to create order for waId={}: {}", waId, e.getMessage(), e);
            client.sendText(waId,
                "😔 Ocorreu um erro ao criar seu pedido. " +
                "Por favor, tente novamente ou entre em contato conosco.\n\n" +
                "Envie qualquer mensagem para recomeçar.");

            state.setCurrentStep(ConversationStep.EXPIRED);
            state.setExpired(true);
            repository.save(state);
        }
    }

    // ---- Special commands ----

    private void handleCancel(ConversationState state) {
        String waId = state.getWaId();
        state.setCurrentStep(ConversationStep.EXPIRED);
        state.setExpired(true);
        repository.save(state);

        client.sendText(waId,
            "❌ Conversa cancelada.\n" +
            "Envie qualquer mensagem para começar uma nova encomenda! 🧸");

        log.info("Conversation cancelled for waId={}", waId);
    }

    private void handleStatus(String waId) {
        Optional<ConversationState> lastCompleted = repository.findLastCompletedByWaId(waId);

        if (lastCompleted.isPresent()) {
            ConversationState completed = lastCompleted.get();
            client.sendText(waId,
                "📋 *Último pedido:*\n\n" +
                "🧸 " + completed.getOrderScope() + "\n" +
                "📝 " + completed.getOrderScopeDetail() + "\n" +
                "📅 " + completed.getFieldValueForStep(ConversationStep.ASK_DATE) + "\n" +
                "✅ Criado em " + formatDateTime(completed.getCompletedAt()));
        } else {
            client.sendText(waId,
                "📋 Nenhum pedido encontrado para este número.\n" +
                "Envie qualquer mensagem para fazer uma encomenda! 🧸");
        }
    }

    private void handleHelp(String waId) {
        client.sendText(waId,
            "💡 *Comandos disponíveis:*\n\n" +
            "📝 /cancelar — Cancela e recomeça\n" +
            "📋 /status — Consulta último pedido\n" +
            "❓ /ajuda — Mostra esta mensagem\n\n" +
            "Basta enviar qualquer mensagem para iniciar ou retomar sua encomenda.");
    }

    // ---- Prompt messages for each step ----

    private void sendPromptForStep(ConversationState state, ConversationStep step) {
        String waId = state.getWaId();

        switch (step) {
            case ASK_NAME -> client.sendText(waId, getPromptText(ConversationStep.ASK_NAME));
            case ASK_EMAIL -> client.sendText(waId, getPromptText(ConversationStep.ASK_EMAIL));
            case ASK_PHONE -> client.sendText(waId, getPromptText(ConversationStep.ASK_PHONE));
            case ASK_ADDRESS -> client.sendText(waId, getPromptText(ConversationStep.ASK_ADDRESS));
            case ASK_POSTAL_CODE -> client.sendText(waId, getPromptText(ConversationStep.ASK_POSTAL_CODE));
            case ASK_ORDER_SCOPE -> client.sendInteractive(waId,
                InteractiveMessageBuilder.orderScopeButtons());
            case ASK_DETAIL -> client.sendText(waId, getPromptText(ConversationStep.ASK_DETAIL));
            case ASK_DATE -> client.sendText(waId, getPromptText(ConversationStep.ASK_DATE));
            case CONFIRM -> resendConfirmation(state);
            default -> {} // terminal steps — no prompt
        }
    }

    static String getPromptText(ConversationStep step) {
        return switch (step) {
            case ASK_NAME -> "Qual é o seu *nome completo*?";
            case ASK_EMAIL -> "📧 Qual é o seu *email*?";
            case ASK_PHONE -> "📱 Qual é o seu *telefone com DDD*? (ex: 11999991234)";
            case ASK_ADDRESS -> "🏠 Qual é o *endereço de entrega*?";
            case ASK_POSTAL_CODE -> "📮 Qual é o *CEP*? (ex: 01310-100)";
            case ASK_ORDER_SCOPE -> "Que *tipo de boneca* você gostaria?";
            case ASK_DETAIL ->
                "📝 Descreva os *detalhes da sua encomenda*:\n" +
                "(cores, tamanho, personagem, referências, etc.)";
            case ASK_DATE -> "📅 Qual a *data desejada para entrega*? (formato: DD/MM/AAAA)";
            default -> "";
        };
    }

    // ---- Helpers ----

    private ConversationState createNewConversation(String waId) {
        ConversationState state = new ConversationState();
        state.setWaId(waId);
        state.setCurrentStep(ConversationStep.GREETING);
        return repository.save(state);
    }

    private void resendConfirmation(ConversationState state) {
        String summary = InteractiveMessageBuilder.buildSummaryText(state);
        Map<String, Object> buttons = InteractiveMessageBuilder.confirmationButtons(summary);
        client.sendInteractive(state.getWaId(), buttons);
        repository.save(state);
    }

    private static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "—";
        return String.format("%02d/%02d/%04d %02d:%02d",
            dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear(),
            dt.getHour(), dt.getMinute());
    }
}
