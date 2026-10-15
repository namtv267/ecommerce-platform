package com.shop.order.domain.exception;

public class ProductCatalogUnavailableException extends DomainException {
    public ProductCatalogUnavailableException(Throwable cause) {
        super("Product catalog is currently unavailable", cause);
    }
}
