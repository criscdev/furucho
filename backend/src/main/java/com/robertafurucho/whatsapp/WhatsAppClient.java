package com.robertafurucho.whatsapp;

import com.robertafurucho.whatsapp.message.InteractiveMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP client for sending messages via the WhatsApp Cloud API.
 *
 * <p>Wraps the {@code POST /{phone-number-id}/messages} endpoint.
 * All messages are sent as non-template messages within the Customer
 * Service Window (free tier).
 *
 * <p>Includes exponential backoff retry (3 attempts, 1–5 s jitter)
 * for transient failures (5xx, timeouts). 4xx errors are not retried.
 */
@Component
public class WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppClient.class);

    private final WebClient webClient;
    private final WhatsAppConfig config;

    public WhatsAppClient(WhatsAppConfig config) {
        this.config = config;
        String baseUrl = Objects.requireNonNull(
            config.getApi().getBaseUrl(),
            "whatsapp.api.base-url must not be null"
        );
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
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
        String phoneNumberId = Objects.requireNonNull(
            config.getApi().getPhoneNumberId(),
            "whatsapp.api.phone-number-id must not be null"
        );
        String accessToken = Objects.requireNonNull(
            config.getApi().getAccessToken(),
            "whatsapp.api.access-token must not be null"
        );
        Object payload = Objects.requireNonNull(body, "whatsapp payload must not be null");

        webClient.post()
            .uri("/{phoneNumberId}/messages", phoneNumberId)
            .header("Authorization", "Bearer " + accessToken)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(5))
                .jitter(0.5)
                .filter(this::isRetryable)
                .doBeforeRetry(signal ->
                    log.warn("Retrying WhatsApp API call (attempt {}): {}",
                        signal.totalRetries() + 1, signal.failure().getMessage())))
            .doOnSuccess(resp -> log.debug("WhatsApp API response: {}", resp))
            .doOnError(err -> log.error("WhatsApp API error after retries: {}", err.getMessage()))
            .subscribe(
                resp -> {},
                err -> log.error("WhatsApp API call failed permanently: {}", err.getMessage())
            );
    }

    /**
     * Returns true if the error is retryable (server errors, timeouts).
     * Client errors (4xx) are not retried — they indicate bad requests.
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError();
        }
        // Retry on connection/timeout errors
        return true;
    }
}
