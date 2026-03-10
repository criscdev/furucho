package com.robertafurucho.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Validates HMAC-SHA256 signatures on incoming WhatsApp webhooks.
 *
 * <p>Meta signs every webhook POST with the App Secret using HMAC-SHA256.
 * The signature arrives in the {@code X-Hub-Signature-256} header as
 * {@code sha256=<hex>}. This component verifies it to prevent spoofing.
 */
@Component
public class WhatsAppSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSignatureValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final WhatsAppConfig config;

    public WhatsAppSignatureValidator(WhatsAppConfig config) {
        this.config = config;
    }

    /**
     * Validates the webhook signature against the raw request body.
     *
     * @param payload   the raw request body bytes
     * @param signature the X-Hub-Signature-256 header value
     * @return true if signature is valid
     */
    public boolean isValid(byte[] payload, String signature) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Missing or malformed X-Hub-Signature-256 header");
            return false;
        }

        String appSecret = config.getAppSecret();
        if (appSecret == null || appSecret.isBlank()) {
            log.error("WhatsApp App Secret not configured — cannot validate webhook");
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] expectedHash = mac.doFinal(payload);

            String expectedHex = SIGNATURE_PREFIX + HexFormat.of().formatHex(expectedHash);
            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256 for webhook validation", e);
            return false;
        }
    }
}
