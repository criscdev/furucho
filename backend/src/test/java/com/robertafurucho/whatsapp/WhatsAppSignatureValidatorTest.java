package com.robertafurucho.whatsapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WhatsAppSignatureValidator}.
 */
@DisplayName("WhatsAppSignatureValidator")
class WhatsAppSignatureValidatorTest {

    private static final String APP_SECRET = "test-app-secret-key";
    private WhatsAppSignatureValidator validator;

    @BeforeEach
    void setUp() {
        WhatsAppConfig config = mock(WhatsAppConfig.class);
        when(config.getAppSecret()).thenReturn(APP_SECRET);
        validator = new WhatsAppSignatureValidator(config);
    }

    @Test
    @DisplayName("accepts a valid HMAC-SHA256 signature")
    void validSignature() throws Exception {
        byte[] payload = "{\"test\":\"data\"}".getBytes(StandardCharsets.UTF_8);
        String signature = computeSignature(payload, APP_SECRET);

        assertThat(validator.isValid(payload, signature)).isTrue();
    }

    @Test
    @DisplayName("rejects an invalid HMAC-SHA256 signature")
    void invalidSignature() {
        byte[] payload = "{\"test\":\"data\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.isValid(payload, "sha256=deadbeef")).isFalse();
    }

    @Test
    @DisplayName("rejects null signature")
    void nullSignature() {
        byte[] payload = "body".getBytes(StandardCharsets.UTF_8);
        assertThat(validator.isValid(payload, null)).isFalse();
    }

    @Test
    @DisplayName("rejects signature without sha256= prefix")
    void noPrefixSignature() {
        byte[] payload = "body".getBytes(StandardCharsets.UTF_8);
        assertThat(validator.isValid(payload, "abc123")).isFalse();
    }

    @Test
    @DisplayName("rejects when app secret is not configured")
    void noAppSecret() {
        WhatsAppConfig config = mock(WhatsAppConfig.class);
        when(config.getAppSecret()).thenReturn("");
        WhatsAppSignatureValidator noSecretValidator = new WhatsAppSignatureValidator(config);

        byte[] payload = "body".getBytes(StandardCharsets.UTF_8);
        assertThat(noSecretValidator.isValid(payload, "sha256=abc")).isFalse();
    }

    private static String computeSignature(byte[] payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload);
        return "sha256=" + HexFormat.of().formatHex(hash);
    }
}
