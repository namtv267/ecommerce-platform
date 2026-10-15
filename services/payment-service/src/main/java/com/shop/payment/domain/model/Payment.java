package com.shop.payment.domain.model;

import com.shop.payment.domain.exception.IllegalPaymentStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/** Bất biến (immutable): mỗi chuyển trạng thái trả về bản sao mới. PENDING chỉ đi tới COMPLETED hoặc FAILED. */
public final class Payment {

    private final UUID id;
    private final UUID orderId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final String currency;
    private final String idempotencyKey;
    private final PaymentStatus status;
    private final String gatewayReference;
    private final String failureReason;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Payment(UUID id, UUID orderId, UUID customerId, BigDecimal amount, String currency, String idempotencyKey,
                    PaymentStatus status, String gatewayReference, String failureReason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.gatewayReference = gatewayReference;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment newPending(UUID orderId, UUID customerId, BigDecimal amount, String currency, String idempotencyKey) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        Instant now = Instant.now();
        return new Payment(UUID.randomUUID(), orderId, customerId, amount.setScale(2, RoundingMode.HALF_UP),
                currency, idempotencyKey, PaymentStatus.PENDING, null, null, now, now);
    }

    public static Payment reconstitute(UUID id, UUID orderId, UUID customerId, BigDecimal amount, String currency,
                                       String idempotencyKey, PaymentStatus status, String gatewayReference,
                                       String failureReason, Instant createdAt, Instant updatedAt) {
        return new Payment(id, orderId, customerId, amount, currency, idempotencyKey, status,
                gatewayReference, failureReason, createdAt, updatedAt);
    }

    public Payment complete(String gatewayReference) {
        requirePending(PaymentStatus.COMPLETED);
        return new Payment(id, orderId, customerId, amount, currency, idempotencyKey,
                PaymentStatus.COMPLETED, gatewayReference, null, createdAt, Instant.now());
    }

    public Payment fail(String reason) {
        requirePending(PaymentStatus.FAILED);
        return new Payment(id, orderId, customerId, amount, currency, idempotencyKey,
                PaymentStatus.FAILED, null, reason, createdAt, Instant.now());
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    /** Cùng idempotency key phải mang cùng nội dung; khác nội dung = client dùng sai key. */
    public boolean sameRequest(UUID orderId, BigDecimal amount, String currency) {
        return this.orderId.equals(orderId)
                && this.amount.compareTo(amount.setScale(2, RoundingMode.HALF_UP)) == 0
                && this.currency.equals(currency);
    }

    private void requirePending(PaymentStatus target) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalPaymentStateException(id, status, target);
        }
    }

    public UUID id() { return id; }
    public UUID orderId() { return orderId; }
    public UUID customerId() { return customerId; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public String idempotencyKey() { return idempotencyKey; }
    public PaymentStatus status() { return status; }
    public String gatewayReference() { return gatewayReference; }
    public String failureReason() { return failureReason; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
