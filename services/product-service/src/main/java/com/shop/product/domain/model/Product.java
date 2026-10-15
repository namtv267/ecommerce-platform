package com.shop.product.domain.model;

import java.math.BigDecimal;

public record Product(String id, String sku, String name, String description,
                      BigDecimal price, String currency, int stock, boolean active) {

    /** Kiểm tra tồn kho cơ bản (read-model, không giữ chỗ hàng). */
    public boolean canFulfill(int quantity) {
        return active && stock >= quantity;
    }
}
