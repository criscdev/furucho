-- ===========================================
-- V1: Create orders table
-- ===========================================
-- Stores customer doll-order requests submitted
-- via the web form or the WhatsApp chatbot.

CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    email           VARCHAR(100)    NOT NULL,
    phone           VARCHAR(15)     NOT NULL,
    address         VARCHAR(200)    NOT NULL,
    postal_code     VARCHAR(10)     NOT NULL,
    order_scope     VARCHAR(100)    NOT NULL,
    order_scope_detail VARCHAR(800) NOT NULL,
    receive_date    DATE            NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
);
