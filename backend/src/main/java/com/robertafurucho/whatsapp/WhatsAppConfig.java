package com.robertafurucho.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for WhatsApp Cloud API integration.
 *
 * <p>Bound to the {@code whatsapp.*} prefix in application.properties.
 * All sensitive values resolve from environment variables in production.
 */
@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppConfig {

    private boolean enabled = false;
    private Api api = new Api();
    private Webhook webhook = new Webhook();
    private String appSecret = "";
    private Conversation conversation = new Conversation();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }

    public Webhook getWebhook() { return webhook; }
    public void setWebhook(Webhook webhook) { this.webhook = webhook; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public static class Api {
        private String baseUrl = "https://graph.facebook.com/v25.0";
        private String phoneNumberId = "";
        private String accessToken = "";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getPhoneNumberId() { return phoneNumberId; }
        public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }

    public static class Webhook {
        private String verifyToken = "";

        public String getVerifyToken() { return verifyToken; }
        public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }
    }

    public static class Conversation {
        private int timeoutMinutes = 30;
        private int maxRetriesPerField = 3;

        public int getTimeoutMinutes() { return timeoutMinutes; }
        public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }

        public int getMaxRetriesPerField() { return maxRetriesPerField; }
        public void setMaxRetriesPerField(int maxRetriesPerField) { this.maxRetriesPerField = maxRetriesPerField; }
    }
}
