package com.robertafurucho.whatsapp.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Records modelling the inbound WhatsApp Cloud API webhook payload.
 *
 * <p>Only the fields relevant to the chatbot are mapped — everything
 * else is ignored via {@link JsonIgnoreProperties}.
 */
public final class WhatsAppMessage {

    private WhatsAppMessage() {} // namespace only

    /**
     * Top-level webhook payload.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookPayload(
        String object,
        List<Entry> entry
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
        String id,
        List<Change> changes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(
        Value value,
        String field
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
        @JsonProperty("messaging_product") String messagingProduct,
        Metadata metadata,
        List<Contact> contacts,
        List<Message> messages
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
        @JsonProperty("display_phone_number") String displayPhoneNumber,
        @JsonProperty("phone_number_id") String phoneNumberId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
        Profile profile,
        @JsonProperty("wa_id") String waId
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
        String from,
        String id,
        String timestamp,
        String type,
        TextBody text,
        Interactive interactive
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextBody(String body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Interactive(
        String type,
        @JsonProperty("button_reply") ButtonReply buttonReply,
        @JsonProperty("list_reply") ListReply listReply
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ButtonReply(String id, String title) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListReply(String id, String title, String description) {}
}
