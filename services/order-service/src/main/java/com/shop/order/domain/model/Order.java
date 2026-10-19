package com.shop.order.domain.model;

import com.shop.order.domain.exception.EmptyOrderException;
import com.shop.order.domain.exception.IllegalOrderStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Aggregate root. Mọi chuyển trạng thái đều đi qua method của aggregate để bảo vệ bất biến. */
public final class Order {

    private final UUID id;
    private final UUID customerId;
    private OrderStatus status;
    private final String currency;
    private final BigDecimal totalAmount;
    private String cancelReason;
    private int paymentAttempts;
    private final String idempotencyKey;
    private final List<OrderItem> items;
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(UUID id, UUID customerId, OrderStatus status, String currency, BigDecimal totalAmount,
                  String cancelReason, int paymentAttempts, String idempotencyKey, List<OrderItem> items,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.cancelReason = cancelReason;
        this.paymentAttempts = paymentAttempts;
        this.idempotencyKey = idempotencyKey;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Tạo đơn mới ở trạng thái PENDING; lệnh thanh toán đầu tiên được gửi ngay (attempt = 1). */
    public static Order place(UUID customerId, String idempotencyKey, String currency, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new EmptyOrderException();
        }
        BigDecimal total = items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant now = Instant.now();
        return new Order(UUID.randomUUID(), customerId, OrderStatus.PENDING, currency, total, null, 1,
                idempotencyKey, List.copyOf(items), now, now);
    }

    public static Order reconstitute(UUID id, UUID customerId, OrderStatus status, String currency,
                                     BigDecimal totalAmount, String cancelReason, int paymentAttempts,
                                     String idempotencyKey, List<OrderItem> items, Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, status, currency, totalAmount, cancelReason, paymentAttempts,
                idempotencyKey, List.copyOf(items), createdAt, updatedAt);
    }

    /** @return true nếu vừa chuyển trạng thái; false nếu đã CONFIRMED từ trước (idempotent). */
    public boolean confirm() {
        if (status == OrderStatus.CONFIRMED) {
            return false;
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalOrderStateException(id, status, OrderStatus.CONFIRMED);
        }
        status = OrderStatus.CONFIRMED;
        updatedAt = Instant.now();
        return true;
    }

    /** Bước bù trừ (compensation) của saga. @return false nếu đã CANCELLED từ trước. */
    public boolean cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            return false;
        }
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalOrderStateException(id, status, OrderStatus.CANCELLED);
        }
        status = OrderStatus.CANCELLED;
        cancelReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 255));
        updatedAt = Instant.now();
        return true;
    }

    /** Ghi nhận việc gửi lại lệnh thanh toán (cùng idempotency key nên payment-service không trừ tiền lần 2). */
    public void registerPaymentRetry() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalOrderStateException(id, status, OrderStatus.PENDING);
        }
        paymentAttempts++;
        updatedAt = Instant.now();
    }

    /** Key ổn định theo đơn hàng, dùng cho idempotency ở payment-service. */
    public String paymentIdempotencyKey() {
        return idempotencyKey != null && idempotencyKey.startsWith("email-")
                ? "payment-email-" + id
                : "payment-" + id;
    }

    public UUID id() { return id; }
    public UUID customerId() { return customerId; }
    public OrderStatus status() { return status; }
    public String currency() { return currency; }
    public BigDecimal totalAmount() { return totalAmount; }
    public String cancelReason() { return cancelReason; }
    public int paymentAttempts() { return paymentAttempts; }
    public String idempotencyKey() { return idempotencyKey; }
    public List<OrderItem> items() { return items; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
