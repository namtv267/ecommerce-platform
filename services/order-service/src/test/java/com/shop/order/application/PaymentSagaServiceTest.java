package com.shop.order.application;

import com.shop.contracts.PaymentCompletedEvent;
import com.shop.contracts.PaymentFailedEvent;
import com.shop.order.application.port.out.OrderRepositoryPort;
import com.shop.order.application.port.out.PaymentRequestPort;
import com.shop.order.application.port.out.ProcessedMessagePort;
import com.shop.order.application.service.PaymentSagaService;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderItem;
import com.shop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSagaServiceTest {

    @Mock OrderRepositoryPort orders;
    @Mock PaymentRequestPort paymentRequests;
    @Mock ProcessedMessagePort processed;

    PaymentSagaService saga;
    Order order;

    @BeforeEach
    void setUp() {
        saga = new PaymentSagaService(orders, paymentRequests, processed, new DirectTransaction(),
                Duration.ofSeconds(60), 3);
        order = Order.place(UUID.randomUUID(), "k", "USD",
                List.of(new OrderItem("p1", "Keyboard", new BigDecimal("89.00"), 1)));
    }

    private PaymentCompletedEvent completed() {
        return new PaymentCompletedEvent(order.id(), UUID.randomUUID(), "gw_1", Instant.now());
    }

    @Test
    void paymentCompleted_confirmsOrder() {
        when(processed.markProcessed("m1")).thenReturn(true);
        when(orders.findByIdForUpdate(order.id())).thenReturn(Optional.of(order));

        saga.onPaymentCompleted("m1", completed());

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orders).update(order);
    }

    @Test
    void duplicateMessage_isIgnored() {
        when(processed.markProcessed("m1")).thenReturn(false);

        saga.onPaymentCompleted("m1", completed());

        verifyNoInteractions(orders);
    }

    @Test
    void paymentCompleted_replayedWithNewMessageId_isStillIdempotent() {
        when(processed.markProcessed(any())).thenReturn(true);
        when(orders.findByIdForUpdate(order.id())).thenReturn(Optional.of(order));

        saga.onPaymentCompleted("m1", completed());
        saga.onPaymentCompleted("m2", completed()); // payment-service replay kết quả với message-id mới

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orders).update(order); // chỉ update đúng 1 lần
    }

    @Test
    void paymentFailed_cancelsOrderWithReason() {
        when(processed.markProcessed("m1")).thenReturn(true);
        when(orders.findByIdForUpdate(order.id())).thenReturn(Optional.of(order));

        saga.onPaymentFailed("m1", new PaymentFailedEvent(order.id(), UUID.randomUUID(), "INSUFFICIENT_FUNDS", Instant.now()));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancelReason()).contains("INSUFFICIENT_FUNDS");
        verify(orders).update(order);
    }

    @Test
    void latePayment_onCancelledOrder_doesNotReviveIt() {
        order.cancel("timeout");
        when(processed.markProcessed("m1")).thenReturn(true);
        when(orders.findByIdForUpdate(order.id())).thenReturn(Optional.of(order));

        saga.onPaymentCompleted("m1", completed());

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orders, never()).update(any());
    }

    @Test
    void stalePendingOrder_getsPaymentRequestedAgain() {
        when(orders.findStalePendingIds(any(Instant.class), anyInt())).thenReturn(List.of(order.id()));
        when(orders.findByIdForUpdate(order.id())).thenReturn(Optional.of(order));

        int handled = saga.handleStalePayments();

        assertThat(handled).isEqualTo(1);
        assertThat(order.paymentAttempts()).isEqualTo(2);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRequests).requestPayment(order);
    }

    @Test
    void stalePendingOrder_afterMaxAttempts_isCancelled() {
        order.registerPaymentRetry();
        order.registerPaymentRetry(); // attempts = 3 = max
        when(orders.findStalePendingIds(any(Instant.class), anyInt())).thenReturn(List.of(order.id()));
        when(orders.findByIdForUpdate(order.id())).thenReturn(Optional.of(order));

        saga.handleStalePayments();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentRequests, never()).requestPayment(any());
    }
}
