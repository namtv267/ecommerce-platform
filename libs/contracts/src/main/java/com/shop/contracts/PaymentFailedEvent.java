package com.shop.contracts;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(UUID orderId, UUID paymentId, String reason, Instant failedAt) {
}
