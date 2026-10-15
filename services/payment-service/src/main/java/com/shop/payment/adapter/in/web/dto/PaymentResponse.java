package com.shop.payment.adapter.in.web.dto;

import com.shop.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount, String currency, String status,
                              String gatewayReference, String failureReason, Instant createdAt, Instant updatedAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.id(), p.orderId(), p.amount(), p.currency(), p.status().name(),
                p.gatewayReference(), p.failureReason(), p.createdAt(), p.updatedAt());
    }
}
