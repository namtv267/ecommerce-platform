package com.shop.order.application.service;

import com.shop.contracts.PaymentCompletedEvent;
import com.shop.contracts.PaymentFailedEvent;
import com.shop.order.application.port.in.PaymentResultUseCase;
import com.shop.order.application.port.in.PaymentTimeoutUseCase;
import com.shop.order.application.port.out.OrderRepositoryPort;
import com.shop.order.application.port.out.PaymentRequestPort;
import com.shop.order.application.port.out.ProcessedMessagePort;
import com.shop.order.application.port.out.TransactionPort;
import com.shop.order.domain.exception.OrderNotFoundException;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Saga orchestrator (phần phản ứng). order-service giữ state machine:
 *   PENDING --PaymentCompleted--> CONFIRMED
 *   PENDING --PaymentFailed / hết lượt retry--> CANCELLED   (compensation)
 */
public class PaymentSagaService implements PaymentResultUseCase, PaymentTimeoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaService.class);
    private static final int BATCH = 50;

    private final OrderRepositoryPort orders;
    private final PaymentRequestPort paymentRequests;
    private final ProcessedMessagePort processedMessages;
    private final TransactionPort tx;
    private final Duration paymentTimeout;
    private final int maxPaymentAttempts;

    public PaymentSagaService(OrderRepositoryPort orders, PaymentRequestPort paymentRequests,
                              ProcessedMessagePort processedMessages, TransactionPort tx,
                              Duration paymentTimeout, int maxPaymentAttempts) {
        this.orders = orders;
        this.paymentRequests = paymentRequests;
        this.processedMessages = processedMessages;
        this.tx = tx;
        this.paymentTimeout = paymentTimeout;
        this.maxPaymentAttempts = maxPaymentAttempts;
    }

    @Override
    public void onPaymentCompleted(String messageId, PaymentCompletedEvent event) {
        tx.run(() -> {
            if (!processedMessages.markProcessed(messageId)) {
                log.info("Duplicate message {} ignored", messageId);
                return;
            }
            Order order = lock(event.orderId());
            if (order.status() == OrderStatus.CANCELLED) {
                // Đơn đã bị huỷ (vd. do timeout) nhưng tiền đã thu -> cần hoàn tiền/đối soát thủ công
                log.warn("LATE PAYMENT for cancelled order {} (payment {}): refund required",
                        order.id(), event.paymentId());
                return;
            }
            if (order.confirm()) {
                orders.update(order);
                log.info("Order {} CONFIRMED (payment {})", order.id(), event.paymentId());
            }
        });
    }

    @Override
    public void onPaymentFailed(String messageId, PaymentFailedEvent event) {
        tx.run(() -> {
            if (!processedMessages.markProcessed(messageId)) {
                log.info("Duplicate message {} ignored", messageId);
                return;
            }
            Order order = lock(event.orderId());
            if (order.status() == OrderStatus.CONFIRMED) {
                log.warn("Ignoring PaymentFailed for already CONFIRMED order {}", order.id());
                return;
            }
            if (order.cancel("Payment failed: " + event.reason())) {
                orders.update(order);
                log.info("Order {} CANCELLED ({})", order.id(), event.reason());
            }
        });
    }

    @Override
    public int handleStalePayments() {
        List<UUID> staleIds = orders.findStalePendingIds(Instant.now().minus(paymentTimeout), BATCH);
        int handled = 0;
        for (UUID id : staleIds) {
            if (Boolean.TRUE.equals(tx.execute(() -> resendOrCancel(id)))) {
                handled++;
            }
        }
        return handled;
    }

    private boolean resendOrCancel(UUID orderId) {
        Order order = orders.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.status() != OrderStatus.PENDING) {
            return false; // kết quả đã về trong lúc ta chờ khoá
        }
        if (order.paymentAttempts() >= maxPaymentAttempts) {
            order.cancel("Payment not confirmed after " + order.paymentAttempts() + " attempts");
            orders.update(order);
            log.warn("Order {} CANCELLED: payment timed out", order.id());
        } else {
            order.registerPaymentRetry();
            orders.update(order);
            paymentRequests.requestPayment(order); // cùng idempotency key -> payment-service không trừ tiền lần 2
            log.info("Order {} payment re-requested (attempt {})", order.id(), order.paymentAttempts());
        }
        return true;
    }

    private Order lock(UUID orderId) {
        return orders.findByIdForUpdate(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
