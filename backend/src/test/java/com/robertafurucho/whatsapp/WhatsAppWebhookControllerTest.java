package com.robertafurucho.whatsapp;

import com.robertafurucho.whatsapp.conversation.ConversationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link WhatsAppWebhookController}.
 */
@WebMvcTest(WhatsAppWebhookController.class)
@DisplayName("WhatsAppWebhookController")
class WhatsAppWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WhatsAppConfig config;

    @MockitoBean
    private WhatsAppSignatureValidator signatureValidator;

    @MockitoBean
    private ConversationService conversationService;

    // ── GET /api/webhooks/whatsapp (verification) ─

    @Nested
    @DisplayName("GET verification")
    class Verification {

        @Test
        @DisplayName("returns challenge when token matches")
        void validVerification() throws Exception {
            when(config.isEnabled()).thenReturn(true);
            WhatsAppConfig.Webhook webhook = new WhatsAppConfig.Webhook();
            webhook.setVerifyToken("my-token");
            when(config.getWebhook()).thenReturn(webhook);

            mockMvc.perform(get("/api/webhooks/whatsapp")
                    .param("hub.mode", "subscribe")
                    .param("hub.verify_token", "my-token")
                    .param("hub.challenge", "challenge_abc"))
                .andExpect(status().isOk())
                .andExpect(content().string("challenge_abc"));
        }

        @Test
        @DisplayName("returns 403 when token does not match")
        void invalidToken() throws Exception {
            when(config.isEnabled()).thenReturn(true);
            WhatsAppConfig.Webhook webhook = new WhatsAppConfig.Webhook();
            webhook.setVerifyToken("my-token");
            when(config.getWebhook()).thenReturn(webhook);

            mockMvc.perform(get("/api/webhooks/whatsapp")
                    .param("hub.mode", "subscribe")
                    .param("hub.verify_token", "wrong-token")
                    .param("hub.challenge", "challenge_abc"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when feature is disabled")
        void disabledFeature() throws Exception {
            when(config.isEnabled()).thenReturn(false);

            mockMvc.perform(get("/api/webhooks/whatsapp")
                    .param("hub.mode", "subscribe")
                    .param("hub.verify_token", "my-token")
                    .param("hub.challenge", "challenge_abc"))
                .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/webhooks/whatsapp (messages) ────

    @Nested
    @DisplayName("POST inbound messages")
    class InboundMessages {

        private static final String WEBHOOK_PAYLOAD = """
            {
                "object": "whatsapp_business_account",
                "entry": [{
                    "id": "123",
                    "changes": [{
                        "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                                "display_phone_number": "5511999990000",
                                "phone_number_id": "phone123"
                            },
                            "contacts": [{"profile": {"name": "Test"}, "wa_id": "5511999991234"}],
                            "messages": [{
                                "from": "5511999991234",
                                "id": "wamid.abc123",
                                "timestamp": "1234567890",
                                "type": "text",
                                "text": {"body": "Oi"}
                            }]
                        },
                        "field": "messages"
                    }]
                }]
            }
            """;

        @Test
        @DisplayName("processes valid signed payload and returns 200")
        void validPayload() throws Exception {
            when(config.isEnabled()).thenReturn(true);
            when(signatureValidator.isValid(any(byte[].class), anyString())).thenReturn(true);

            mockMvc.perform(post("/api/webhooks/whatsapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(WEBHOOK_PAYLOAD)
                    .header("X-Hub-Signature-256", "sha256=valid"))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));

            verify(conversationService).processMessage(
                eq("5511999991234"), eq("wamid.abc123"), eq("text"), eq("Oi"), isNull());
        }

        @Test
        @DisplayName("rejects invalid signature with 401")
        void invalidSignature() throws Exception {
            when(config.isEnabled()).thenReturn(true);
            when(signatureValidator.isValid(any(byte[].class), anyString())).thenReturn(false);

            mockMvc.perform(post("/api/webhooks/whatsapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(WEBHOOK_PAYLOAD)
                    .header("X-Hub-Signature-256", "sha256=bad"))
                .andExpect(status().isUnauthorized());

            verify(conversationService, never()).processMessage(
                anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("returns 404 when feature is disabled")
        void disabledFeature() throws Exception {
            when(config.isEnabled()).thenReturn(false);

            mockMvc.perform(post("/api/webhooks/whatsapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(WEBHOOK_PAYLOAD)
                    .header("X-Hub-Signature-256", "sha256=valid"))
                .andExpect(status().isNotFound());
        }
    }
}
