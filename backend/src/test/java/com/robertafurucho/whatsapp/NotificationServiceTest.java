package com.robertafurucho.whatsapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationService}.
 *
 * Covers:
 * - Skip when WhatsApp disabled
 * - Skip when robertaWaId blank/null
 * - Happy-path message formatting
 * - safe() null handling
 * - truncate() at boundary and beyond
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private WhatsAppClient client;

    @Mock
    private WhatsAppConfig config;

    @Mock
    private WhatsAppConfig.Notification notificationConfig;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(client, config);
    }

    @Nested
    @DisplayName("notifyNewOrder()")
    class NotifyNewOrder {

        @Test
        @DisplayName("skips notification when WhatsApp is disabled")
        void skipsWhenDisabled() {
            when(config.isEnabled()).thenReturn(false);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn("5511999990000");

            service.notifyNewOrder(1L, "Maria", "Boneca", "Detalhe");

            verify(client, never()).sendText(anyString(), anyString());
        }

        @Test
        @DisplayName("skips notification when robertaWaId is null")
        void skipsWhenWaIdNull() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn(null);

            service.notifyNewOrder(1L, "Maria", "Boneca", "Detalhe");

            verify(client, never()).sendText(anyString(), anyString());
        }

        @Test
        @DisplayName("skips notification when robertaWaId is blank")
        void skipsWhenWaIdBlank() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn("   ");

            service.notifyNewOrder(1L, "Maria", "Boneca", "Detalhe");

            verify(client, never()).sendText(anyString(), anyString());
        }

        @Test
        @DisplayName("sends formatted message to Roberta on happy path")
        void sendsFormattedMessage() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn("5511999990000");

            service.notifyNewOrder(42L, "Maria da Silva", "Bailarina",
                    "Vestido azul com detalhes em prata");

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(client).sendText(eq("5511999990000"), msgCaptor.capture());

            String msg = msgCaptor.getValue();
            assertThat(msg).contains("Novo pedido #42");
            assertThat(msg).contains("Maria da Silva");
            assertThat(msg).contains("Bailarina");
            assertThat(msg).contains("Vestido azul com detalhes em prata");
            assertThat(msg).contains("painel");
        }

        @Test
        @DisplayName("handles null customer name with safe() fallback")
        void handlesNullCustomerName() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn("5511999990000");

            service.notifyNewOrder(1L, null, "Boneca", "Detalhe");

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(client).sendText(eq("5511999990000"), msgCaptor.capture());

            assertThat(msgCaptor.getValue()).contains("—");
        }

        @Test
        @DisplayName("handles null orderScopeDetail with safe() fallback")
        void handlesNullOrderScopeDetail() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn("5511999990000");

            service.notifyNewOrder(1L, "Maria", "Boneca", null);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(client).sendText(eq("5511999990000"), msgCaptor.capture());

            // safe(null) = "—", truncate("—", 200) → "—" (1 char ≤ 200)
            assertThat(msgCaptor.getValue()).contains("—");
        }

        @Test
        @DisplayName("truncates long orderScopeDetail to 200 chars with ellipsis")
        void truncatesLongDetail() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn("5511999990000");

            String longDetail = "A".repeat(250);
            service.notifyNewOrder(1L, "Maria", "Boneca", longDetail);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(client).sendText(eq("5511999990000"), msgCaptor.capture());

            String msg = msgCaptor.getValue();
            // Truncated: first 197 chars + "..."
            assertThat(msg).contains("A".repeat(197) + "...");
            assertThat(msg).doesNotContain("A".repeat(198));
        }

        @Test
        @DisplayName("does not truncate detail exactly at 200 chars")
        void doesNotTruncateAtBoundary() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getNotification()).thenReturn(notificationConfig);
            when(notificationConfig.getRobertaWaId()).thenReturn("5511999990000");

            String exactDetail = "B".repeat(200);
            service.notifyNewOrder(1L, "Maria", "Boneca", exactDetail);

            ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
            verify(client).sendText(eq("5511999990000"), msgCaptor.capture());

            assertThat(msgCaptor.getValue()).contains("B".repeat(200));
        }
    }
}
