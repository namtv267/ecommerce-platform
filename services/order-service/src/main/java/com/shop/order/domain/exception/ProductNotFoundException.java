package com.shop.order.domain.exception;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(String productId) {
        super("Product not found or inactive: " + productId);
    }
}
