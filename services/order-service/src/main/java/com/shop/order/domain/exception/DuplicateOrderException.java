package com.shop.order.domain.exception;

/** Vi phạm unique (customer_id, idempotency_key): đã có đơn được tạo với key này. */
public class DuplicateOrderException extends DomainException {
    public DuplicateOrderException(String idempotencyKey, Throwable cause) {
        super("Order already exists for idempotency key " + idempotencyKey, cause);
    }
}
