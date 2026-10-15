package com.shop.product.adapter.in.web.dto;

import com.shop.product.domain.model.Product;

import java.math.BigDecimal;

public record ProductResponse(String id, String sku, String name, String description,
                              BigDecimal price, String currency, int stock, boolean active) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.id(), p.sku(), p.name(), p.description(),
                p.price(), p.currency(), p.stock(), p.active());
    }
}
