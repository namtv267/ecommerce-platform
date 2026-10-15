package com.shop.order.domain.model;

import java.math.BigDecimal;

/** Snapshot giá tại thời điểm đặt hàng (giá sản phẩm đổi sau đó không ảnh hưởng đơn cũ). */
public record OrderItem(String productId, String productName, BigDecimal unitPrice, int quantity) {

    public OrderItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must not be negative");
        }
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
