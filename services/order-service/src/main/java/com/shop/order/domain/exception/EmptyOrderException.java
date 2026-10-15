package com.shop.order.domain.exception;

public class EmptyOrderException extends DomainException {
    public EmptyOrderException() {
        super("Order must contain at least one item");
    }
}
