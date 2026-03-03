-- ===========================================
-- V3: Unique partial index — one active conversation per wa_id
-- ===========================================
-- Prevents race conditions from creating duplicate active
-- conversations for the same WhatsApp user.

CREATE UNIQUE INDEX uq_conversation_states_active_wa_id
    ON conversation_states (wa_id)
    WHERE current_step NOT IN ('COMPLETED', 'EXPIRED') AND expired = FALSE;
