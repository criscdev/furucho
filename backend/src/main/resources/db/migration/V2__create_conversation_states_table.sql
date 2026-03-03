-- ===========================================
-- V2: Create conversation_states table
-- ===========================================
-- Tracks the state of WhatsApp order conversations.
-- Each row represents one active or completed chatbot session.

CREATE TABLE IF NOT EXISTS conversation_states (
    id                  BIGSERIAL       PRIMARY KEY,
    version             BIGINT          NOT NULL DEFAULT 0,
    wa_id               VARCHAR(20)     NOT NULL,
    current_step        VARCHAR(30)     NOT NULL DEFAULT 'GREETING',
    retry_count         INTEGER         NOT NULL DEFAULT 0,
    last_message_id     VARCHAR(100),
    name                VARCHAR(200),
    email               VARCHAR(100),
    phone               VARCHAR(15),
    address             VARCHAR(200),
    postal_code         VARCHAR(10),
    order_scope         VARCHAR(100),
    order_scope_detail  VARCHAR(800),
    receive_date        DATE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMP,
    expired             BOOLEAN         NOT NULL DEFAULT FALSE,
    correcting          BOOLEAN         NOT NULL DEFAULT FALSE
);

-- Index on wa_id for fast lookups by WhatsApp ID
CREATE INDEX idx_conversation_states_wa_id
    ON conversation_states (wa_id);

-- Index on updated_at for the expiration job query
CREATE INDEX idx_conversation_states_updated_at
    ON conversation_states (updated_at)
    WHERE current_step NOT IN ('COMPLETED', 'EXPIRED') AND expired = FALSE;
