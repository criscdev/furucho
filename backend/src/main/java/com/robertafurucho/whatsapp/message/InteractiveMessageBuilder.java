package com.robertafurucho.whatsapp.message;

import com.robertafurucho.whatsapp.conversation.ConversationState;
import com.robertafurucho.whatsapp.conversation.ConversationStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for WhatsApp Cloud API interactive messages.
 *
 * <p>Supports button replies (max 3 buttons) and list replies
 * as defined in the WhatsApp Business API.
 */
public final class InteractiveMessageBuilder {

    private InteractiveMessageBuilder() {} // static utility

    /**
     * Builds the interactive button message for doll type selection (orderScope).
     */
    public static Map<String, Object> orderScopeButtons() {
        return buttonMessage(
            "Que *tipo de boneca* você gostaria?",
            List.of(
                button("tipo_amigurumi", "Amigurumi"),
                button("tipo_pano", "Boneca de Pano"),
                button("tipo_outro", "Outro tipo")
            )
        );
    }

    /**
     * Builds the confirmation buttons after order summary.
     *
     * @param summaryText the formatted order summary
     */
    public static Map<String, Object> confirmationButtons(String summaryText) {
        return buttonMessage(
            summaryText,
            List.of(
                button("confirm_yes", "✓ Confirmar"),
                button("confirm_edit", "✏ Corrigir"),
                button("confirm_cancel", "✗ Cancelar")
            )
        );
    }

    /**
     * Builds the correction field selection list.
     *
     * @param state the current conversation state (to show current values)
     */
    public static Map<String, Object> correctionList(ConversationState state) {
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row("fix_name", "Nome", truncate(state.getName())));
        rows.add(row("fix_email", "Email", truncate(state.getEmail())));
        rows.add(row("fix_phone", "Telefone", truncate(state.getPhone())));
        rows.add(row("fix_address", "Endereço", truncate(state.getAddress())));
        rows.add(row("fix_cep", "CEP", truncate(state.getPostalCode())));
        rows.add(row("fix_scope", "Tipo de boneca", truncate(state.getOrderScope())));
        rows.add(row("fix_detail", "Detalhes", truncate(state.getOrderScopeDetail())));
        rows.add(row("fix_date", "Data de entrega",
            truncate(state.getFieldValueForStep(ConversationStep.ASK_DATE))));

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("title", "Campos");
        section.put("rows", rows);

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("button", "Selecionar campo");
        action.put("sections", List.of(section));

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "list");
        interactive.put("body", Map.of("text", "Qual campo deseja corrigir?"));
        interactive.put("action", action);

        return interactive;
    }

    /**
     * Builds the formatted order summary text.
     */
    public static String buildSummaryText(ConversationState state) {
        return """
            🧸 *Resumo do seu Pedido*
            
            *Nome:* %s
            *Email:* %s
            *Telefone:* %s
            *Endereço:* %s
            *CEP:* %s
            
            *Tipo:* %s
            *Detalhes:* %s
            *Data desejada:* %s
            
            Está tudo certo? 👇"""
            .formatted(
                safe(state.getName()),
                safe(state.getEmail()),
                safe(state.getPhone()),
                safe(state.getAddress()),
                safe(state.getPostalCode()),
                safe(state.getOrderScope()),
                safe(state.getOrderScopeDetail()),
                safe(state.getFieldValueForStep(ConversationStep.ASK_DATE))
            );
    }

    // --- Private helpers ---

    private static Map<String, Object> buttonMessage(String bodyText, List<Map<String, Object>> buttons) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("buttons", buttons);

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "button");
        interactive.put("body", Map.of("text", bodyText));
        interactive.put("action", action);

        return interactive;
    }

    private static Map<String, Object> button(String id, String title) {
        return Map.of(
            "type", "reply",
            "reply", Map.of("id", id, "title", title)
        );
    }

    private static Map<String, String> row(String id, String title, String description) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("title", title);
        if (description != null && !description.isBlank()) {
            r.put("description", "Atual: " + description);
        }
        return r;
    }

    private static String truncate(String value) {
        if (value == null) return "";
        return value.length() > 50 ? value.substring(0, 47) + "..." : value;
    }

    private static String safe(String value) {
        return value != null ? value : "—";
    }
}
