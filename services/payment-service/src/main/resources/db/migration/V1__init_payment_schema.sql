CREATE TABLE payments (
    id                UUID          PRIMARY KEY,
    order_id          UUID          NOT NULL,
    customer_id       UUID          NOT NULL,
    amount            NUMERIC(19,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    idempotency_key   VARCHAR(128)  NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    gateway_reference VARCHAR(100),
    failure_reason    VARCHAR(255),
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    -- Idempotency tầng nghiệp vụ: cùng key (hoặc cùng đơn) không bao giờ có 2 bản ghi thanh toán
    CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uq_payments_order UNIQUE (order_id)
);

-- Inbox: khử trùng lặp message Kafka
CREATE TABLE processed_messages (
    message_id   VARCHAR(64) PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

-- Outbox: kết quả thanh toán chờ gửi, ghi cùng transaction với việc cập nhật payment
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
