package com.robertafurucho.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends WhatsApp notifications to Roberta when business events occur.
 *
 * <p>Currently supports:
 * <ul>
 *   <li>New order created via chatbot → alert Roberta with order details</li>
 * </ul>
 *
 * <p>Notifications are only sent when {@code whatsapp.enabled=true} and
 * {@code whatsapp.notification.roberta-wa-id} is configured.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final WhatsAppClient client;
    private final WhatsAppConfig config;

    public NotificationService(WhatsAppClient client, WhatsAppConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Notifies Roberta that a new order was created via the WhatsApp chatbot.
     *
     * @param orderId         the created order ID
     * @param customerName    the customer's name
     * @param orderScope      the doll type ordered
     * @param orderScopeDetail the order description
     */
    public void notifyNewOrder(Long orderId, String customerName,
                               String orderScope, String orderScopeDetail) {
        String robertaWaId = config.getNotification().getRobertaWaId();

        if (!config.isEnabled() || robertaWaId == null || robertaWaId.isBlank()) {
            log.debug("Skipping Roberta notification — disabled or no waId configured");
            return;
        }

        String message = String.format(
            "🔔 *Novo pedido #%d!*\n\n" +
            "👤 *Cliente:* %s\n" +
            "🧸 *Tipo:* %s\n" +
            "📝 *Detalhes:* %s\n\n" +
            "Acesse o painel para ver todos os detalhes.",
            orderId,
            safe(customerName),
            safe(orderScope),
            truncate(safe(orderScopeDetail), 200)
        );

        client.sendText(robertaWaId, message);
        log.info("New order notification sent to Roberta for order #{}", orderId);
    }

    private static String safe(String value) {
        return value != null ? value : "—";
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
