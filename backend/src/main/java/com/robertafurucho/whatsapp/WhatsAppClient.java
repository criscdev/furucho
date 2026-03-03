package com.robertafurucho.whatsapp;

import com.robertafurucho.whatsapp.message.InteractiveMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * HTTP client for sending messages via the WhatsApp Cloud API.
 *
 * <p>Wraps the {@code POST /{phone-number-id}/messages} endpoint.
 * All messages are sent as non-template messages within the Customer
 * Service Window (free tier).
 */
@Component
public class WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppClient.class);

    private final WebClient webClient;
    private final WhatsAppConfig config;

    public WhatsAppClient(WhatsAppConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
            .baseUrl(config.getApi().getBaseUrl())
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Sends a plain text message to a WhatsApp user.
     *
     * @param to   the recipient WhatsApp ID (e.g. "5511999991234")
     * @param text the message body
     */
    public void sendText(String to, String text) {
        if (!config.isEnabled()) {
            log.debug("WhatsApp disabled — would send to {}: {}", to, text);
            return;
        }

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", to,
            "type", "text",
            "text", Map.of("body", text)
        );

        sendMessage(body);
    }

    /**
     * Sends an interactive button message.
     *
     * @param to      the recipient
     * @param payload the full interactive message payload (built by {@link InteractiveMessageBuilder})
     */
    public void sendInteractive(String to, Map<String, Object> payload) {
        if (!config.isEnabled()) {
            log.debug("WhatsApp disabled — would send interactive to {}", to);
            return;
        }

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", to,
            "type", "interactive",
            "interactive", payload
        );

        sendMessage(body);
    }

    private void sendMessage(Map<String, Object> body) {
        String phoneNumberId = config.getApi().getPhoneNumberId();
        String accessToken = config.getApi().getAccessToken();

        webClient.post()
            .uri("/{phoneNumberId}/messages", phoneNumberId)
            .header("Authorization", "Bearer " + accessToken)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(resp -> log.debug("WhatsApp API response: {}", resp))
            .doOnError(err -> log.error("WhatsApp API error: {}", err.getMessage()))
            .subscribe(
                resp -> {},
                err -> log.error("WhatsApp API call failed: {}", err.getMessage())
            );
    }
}
