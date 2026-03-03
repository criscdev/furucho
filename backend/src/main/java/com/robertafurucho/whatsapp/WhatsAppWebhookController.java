package com.robertafurucho.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.robertafurucho.whatsapp.message.WhatsAppMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for WhatsApp Cloud API webhooks.
 *
 * <p>Handles two endpoints:
 * <ul>
 *   <li><strong>GET</strong> — Webhook verification (Meta challenge handshake)</li>
 *   <li><strong>POST</strong> — Inbound message processing with HMAC-SHA256 validation</li>
 * </ul>
 *
 * <p>The POST handler returns 200 immediately and delegates message
 * processing to {@link WhatsAppMessageProcessor} on a background thread,
 * ensuring compliance with Meta's 5-second response requirement.
 */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppConfig config;
    private final WhatsAppSignatureValidator signatureValidator;
    private final WhatsAppMessageProcessor messageProcessor;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(
            WhatsAppConfig config,
            WhatsAppSignatureValidator signatureValidator,
            WhatsAppMessageProcessor messageProcessor,
            ObjectMapper objectMapper) {
        this.config = config;
        this.signatureValidator = signatureValidator;
        this.messageProcessor = messageProcessor;
        this.objectMapper = objectMapper;
    }

    /**
     * Webhook verification endpoint (Meta challenge handshake).
     *
     * <p>When registering the webhook URL, Meta sends a GET request with:
     * <ul>
     *   <li>{@code hub.mode=subscribe}</li>
     *   <li>{@code hub.verify_token=<your-verify-token>}</li>
     *   <li>{@code hub.challenge=<random-string>}</li>
     * </ul>
     * We must return the challenge value if the token matches.
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if (!config.isEnabled()) {
            return ResponseEntity.notFound().build();
        }

        if ("subscribe".equals(mode) && config.getWebhook().getVerifyToken().equals(token)) {
            log.info("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        log.warn("Webhook verification failed: mode={}, token mismatch", mode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * Inbound message endpoint.
     *
     * <p>Validates the HMAC-SHA256 signature, parses the webhook payload,
     * and delegates each inbound message to the conversation service.
     * Always returns 200 to acknowledge receipt (Meta retries on non-2xx).
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody byte[] body,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        if (!config.isEnabled()) {
            return ResponseEntity.notFound().build();
        }

        // Validate HMAC signature
        if (!signatureValidator.isValid(body, signature)) {
            log.warn("Invalid webhook signature — rejecting");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            WhatsAppMessage.WebhookPayload payload =
                objectMapper.readValue(body, WhatsAppMessage.WebhookPayload.class);

            processPayload(payload);

        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
            // Still return 200 to prevent Meta from retrying
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /**
     * Extracts messages from the webhook payload and enqueues them for async processing.
     * Skips non-message changes (e.g. status updates with field != "messages").
     */
    private void processPayload(WhatsAppMessage.WebhookPayload payload) {
        if (payload == null || payload.entry() == null) return;

        for (WhatsAppMessage.Entry entry : payload.entry()) {
            if (entry.changes() == null) continue;

            for (WhatsAppMessage.Change change : entry.changes()) {
                // Skip non-message webhooks (status updates, errors, etc.)
                if (change.field() != null && !"messages".equals(change.field())) {
                    log.debug("Skipping non-message webhook field: {}", change.field());
                    continue;
                }

                if (change.value() == null || change.value().messages() == null) continue;

                for (WhatsAppMessage.Message message : change.value().messages()) {
                    String waId = message.from();
                    String messageId = message.id();
                    String type = message.type();
                    String textBody = (message.text() != null) ? message.text().body() : null;
                    WhatsAppMessage.Interactive interactive = message.interactive();

                    // Delegate to async processor — returns immediately
                    messageProcessor.processMessage(
                        waId, messageId, type, textBody, interactive);
                }
            }
        }
    }
}
