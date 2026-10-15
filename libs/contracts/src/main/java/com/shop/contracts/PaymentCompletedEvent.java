package com.shop.contracts;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(UUID orderId, UUID paymentId, String gatewayReference, Instant completedAt) {
}
