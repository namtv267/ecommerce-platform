CREATE TABLE orders (
    id               UUID          PRIMARY KEY,
    customer_id      UUID          NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    total_amount     NUMERIC(19,2) NOT NULL,
    cancel_reason    VARCHAR(255),
    payment_attempts INT           NOT NULL DEFAULT 0,
    idempotency_key  VARCHAR(128)  NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    -- Chốt chặn idempotency ở tầng DB: kể cả Redis mất dữ liệu cũng không tạo 2 đơn cho cùng 1 key
    CONSTRAINT uq_orders_customer_idempotency UNIQUE (customer_id, idempotency_key)
);
CREATE INDEX idx_orders_status_updated ON orders (status, updated_at);

CREATE TABLE order_items (
    id           BIGSERIAL     PRIMARY KEY,
    order_id     UUID          NOT NULL REFERENCES orders (id),
    product_id   VARCHAR(64)   NOT NULL,
    product_name VARCHAR(255)  NOT NULL,
    unit_price   NUMERIC(19,2) NOT NULL,
    quantity     INT           NOT NULL CHECK (quantity > 0)
);
CREATE INDEX idx_order_items_order ON order_items (order_id);

-- Inbox: khử trùng lặp message Kafka
CREATE TABLE processed_messages (
    message_id   VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

-- Outbox: message chờ gửi, ghi cùng transaction với nghiệp vụ
CREATE TABLE outbox_events (
    id           UUID         PRIMARY KEY,
    topic        VARCHAR(100) NOT NULL,
    message_key  VARCHAR(100),
    message_type VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
