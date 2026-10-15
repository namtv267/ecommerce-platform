package com.shop.order.domain.exception;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(String productId, int requested, int available) {
        super("Insufficient stock for " + productId + ": requested " + requested + ", available " + available);
    }
}
