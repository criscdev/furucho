package com.robertafurucho.whatsapp.conversation;

/**
 * Steps in the WhatsApp order chatbot conversation flow.
 *
 * <p>Acts as a state machine — each step represents a question
 * or action in the order collection process. The flow mirrors
 * the 8 fields of {@link com.robertafurucho.order.CreateOrderRequest}.
 */
public enum ConversationStep {

    /** Initial greeting + asks for name */
    GREETING,

    /** Waiting for customer name */
    ASK_NAME,

    /** Waiting for email */
    ASK_EMAIL,

    /** Waiting for phone with DDD */
    ASK_PHONE,

    /** Waiting for delivery address */
    ASK_ADDRESS,

    /** Waiting for postal code (CEP) */
    ASK_POSTAL_CODE,

    /** Waiting for doll type selection (interactive buttons) */
    ASK_ORDER_SCOPE,

    /** Waiting for order details description */
    ASK_DETAIL,

    /** Waiting for desired delivery date (DD/MM/YYYY) */
    ASK_DATE,

    /** Order summary displayed — waiting for confirm/edit/cancel */
    CONFIRM,

    /** Customer chose to correct a field — waiting for field selection */
    CORRECTION,

    /** Order created successfully — conversation finished */
    COMPLETED,

    /** Conversation expired due to inactivity */
    EXPIRED;

    /**
     * Returns the next step in the sequential order collection flow.
     *
     * @return the next conversation step
     * @throws IllegalStateException if called on a terminal or non-sequential step
     */
    public ConversationStep next() {
        return switch (this) {
            case GREETING -> ASK_NAME;
            case ASK_NAME -> ASK_EMAIL;
            case ASK_EMAIL -> ASK_PHONE;
            case ASK_PHONE -> ASK_ADDRESS;
            case ASK_ADDRESS -> ASK_POSTAL_CODE;
            case ASK_POSTAL_CODE -> ASK_ORDER_SCOPE;
            case ASK_ORDER_SCOPE -> ASK_DETAIL;
            case ASK_DETAIL -> ASK_DATE;
            case ASK_DATE -> CONFIRM;
            case CONFIRM, CORRECTION, COMPLETED, EXPIRED ->
                throw new IllegalStateException("No sequential next step for " + this);
        };
    }

    /**
     * Returns the step that corresponds to a correction field ID.
     *
     * @param fieldId the interactive list row ID (e.g. "fix_name")
     * @return the step to re-ask
     * @throws IllegalArgumentException if fieldId is unknown
     */
    public static ConversationStep fromCorrectionFieldId(String fieldId) {
        return switch (fieldId) {
            case "fix_name" -> ASK_NAME;
            case "fix_email" -> ASK_EMAIL;
            case "fix_phone" -> ASK_PHONE;
            case "fix_address" -> ASK_ADDRESS;
            case "fix_cep" -> ASK_POSTAL_CODE;
            case "fix_scope" -> ASK_ORDER_SCOPE;
            case "fix_detail" -> ASK_DETAIL;
            case "fix_date" -> ASK_DATE;
            default -> throw new IllegalArgumentException("Unknown correction field: " + fieldId);
        };
    }

    /**
     * Whether this step is a terminal state (conversation done).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == EXPIRED;
    }
}
