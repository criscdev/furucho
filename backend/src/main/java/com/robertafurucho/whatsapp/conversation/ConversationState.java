package com.robertafurucho.whatsapp.conversation;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity tracking the state of a WhatsApp order conversation.
 *
 * <p>Each active conversation collects the same 8 fields as
 * {@link com.robertafurucho.order.CreateOrderRequest}, one step at a time.
 * State persists in the database to survive server restarts.
 */
@Entity
@Table(name = "conversation_states")
public class ConversationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    /** WhatsApp ID of the customer (e.g. "5511999991234") */
    @Column(name = "wa_id", nullable = false, length = 20)
    private String waId;

    /** Current step in the conversation flow */
    @Column(name = "current_step", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ConversationStep currentStep = ConversationStep.GREETING;

    /** Number of consecutive validation failures for the current step */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /** Last WhatsApp message ID processed (for idempotency) */
    @Column(name = "last_message_id", length = 100)
    private String lastMessageId;

    // --- Collected order fields ---

    @Column(length = 200)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "order_scope", length = 100)
    private String orderScope;

    @Column(name = "order_scope_detail", length = 800)
    private String orderScopeDetail;

    @Column(name = "receive_date")
    private LocalDate receiveDate;

    // --- Timestamps ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private boolean expired = false;

    /** True when the user is correcting a single field (should return to CONFIRM after). */
    @Column(nullable = false)
    private boolean correcting = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Convenience methods ---

    /**
     * Sets the value for the current step's field.
     *
     * @param step  the step whose field to set
     * @param value the validated input value
     */
    public void setFieldForStep(ConversationStep step, String value) {
        switch (step) {
            case ASK_NAME -> this.name = value;
            case ASK_EMAIL -> this.email = value;
            case ASK_PHONE -> this.phone = value.replaceAll("\\D", "");
            case ASK_ADDRESS -> this.address = value;
            case ASK_POSTAL_CODE -> this.postalCode = value;
            case ASK_ORDER_SCOPE -> this.orderScope = value;
            case ASK_DETAIL -> this.orderScopeDetail = value;
            case ASK_DATE -> {
                String[] parts = value.split("/");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid date format: " + value);
                }
                this.receiveDate = LocalDate.of(
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[0])
                );
            }
            default -> throw new IllegalArgumentException("Cannot set field for step: " + step);
        }
    }

    /**
     * Returns the current value for a given step's field (for correction display).
     */
    public String getFieldValueForStep(ConversationStep step) {
        return switch (step) {
            case ASK_NAME -> name;
            case ASK_EMAIL -> email;
            case ASK_PHONE -> phone;
            case ASK_ADDRESS -> address;
            case ASK_POSTAL_CODE -> postalCode;
            case ASK_ORDER_SCOPE -> orderScope;
            case ASK_DETAIL -> orderScopeDetail;
            case ASK_DATE -> receiveDate != null
                ? String.format("%02d/%02d/%04d",
                    receiveDate.getDayOfMonth(), receiveDate.getMonthValue(), receiveDate.getYear())
                : null;
            default -> null;
        };
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWaId() { return waId; }
    public void setWaId(String waId) { this.waId = waId; }

    public ConversationStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(ConversationStep currentStep) { this.currentStep = currentStep; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getLastMessageId() { return lastMessageId; }
    public void setLastMessageId(String lastMessageId) { this.lastMessageId = lastMessageId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getOrderScope() { return orderScope; }
    public void setOrderScope(String orderScope) { this.orderScope = orderScope; }

    public String getOrderScopeDetail() { return orderScopeDetail; }
    public void setOrderScopeDetail(String orderScopeDetail) { this.orderScopeDetail = orderScopeDetail; }

    public LocalDate getReceiveDate() { return receiveDate; }
    public void setReceiveDate(LocalDate receiveDate) { this.receiveDate = receiveDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    public boolean isCorrecting() { return correcting; }
    public void setCorrecting(boolean correcting) { this.correcting = correcting; }

    public Long getVersion() { return version; }
}
