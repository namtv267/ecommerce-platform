package com.shop.payment.domain.exception;

/** Cùng idempotency key nhưng khác orderId/số tiền/tiền tệ -> không bao giờ xử lý, đẩy sang DLT để điều tra. */
public class IdempotencyKeyReuseException extends RuntimeException {
    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency key '" + idempotencyKey + "' was already used with a different payment request");
    }
}
