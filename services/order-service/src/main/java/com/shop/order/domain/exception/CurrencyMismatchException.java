package com.shop.order.domain.exception;

public class CurrencyMismatchException extends DomainException {
    public CurrencyMismatchException() {
        super("All items in an order must use the same currency");
    }
}
