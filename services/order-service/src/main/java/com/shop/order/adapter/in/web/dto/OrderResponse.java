package com.shop.order.adapter.in.web.dto;

import com.shop.order.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, UUID customerId, String status, String currency, BigDecimal totalAmount,
                            String cancelReason, List<ItemResponse> items, Instant createdAt, Instant updatedAt) {

    public record ItemResponse(String productId, String productName, BigDecimal unitPrice, int quantity) {}

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.id(), o.customerId(), o.status().name(), o.currency(), o.totalAmount(),
                o.cancelReason(),
                o.items().stream()
                        .map(i -> new ItemResponse(i.productId(), i.productName(), i.unitPrice(), i.quantity()))
                        .toList(),
                o.createdAt(), o.updatedAt());
    }
}
