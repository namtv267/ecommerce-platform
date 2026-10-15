package com.shop.payment.domain.exception;

/** Vi phạm unique (idempotency_key hoặc order_id): đã có bản ghi thanh toán. */
public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String idempotencyKey, Throwable cause) {
        super("Payment already exists for idempotency key " + idempotencyKey, cause);
    }
}
