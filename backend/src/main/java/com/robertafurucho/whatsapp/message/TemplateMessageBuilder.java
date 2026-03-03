package com.robertafurucho.whatsapp.message;

import java.util.List;
import java.util.Map;

/**
 * Builder for WhatsApp Cloud API template messages.
 *
 * <p>Template messages can be sent outside the 24-hour Customer Service Window
 * and must be pre-approved by Meta. Only two templates are used:
 * <ul>
 *   <li>{@code welcome_order} — marketing, for proactive outreach</li>
 *   <li>{@code order_status_update} — utility, with order ID and status params</li>
 * </ul>
 */
public final class TemplateMessageBuilder {

    private TemplateMessageBuilder() {} // static utility

    /**
     * Builds the welcome/greeting template message payload.
     *
     * @return template payload for the {@code welcome_order} template
     */
    public static Map<String, Object> welcomeTemplate() {
        return Map.of(
            "name", "welcome_order",
            "language", Map.of("code", "pt_BR")
        );
    }

    /**
     * Builds the order status update template message payload.
     *
     * @param orderId the order number
     * @param status  the new status text in Portuguese
     * @return template payload for the {@code order_status_update} template
     */
    public static Map<String, Object> orderStatusUpdateTemplate(Long orderId, String status) {
        return Map.of(
            "name", "order_status_update",
            "language", Map.of("code", "pt_BR"),
            "components", List.of(
                Map.of(
                    "type", "body",
                    "parameters", List.of(
                        Map.of("type", "text", "text", "#" + orderId),
                        Map.of("type", "text", "text", status)
                    )
                )
            )
        );
    }
}
