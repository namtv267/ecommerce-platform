package com.shop.order.application;

import com.shop.order.application.port.in.PlaceOrderUseCase.OrderLine;
import com.shop.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.shop.order.application.port.out.OrderRepositoryPort;
import com.shop.order.application.port.out.PaymentRequestPort;
import com.shop.order.application.port.out.ProductCatalogPort;
import com.shop.order.application.port.out.ProductCatalogPort.CatalogProduct;
import com.shop.order.application.service.OrderService;
import com.shop.order.domain.exception.DuplicateOrderException;
import com.shop.order.domain.exception.InsufficientStockException;
import com.shop.order.domain.exception.ProductNotFoundException;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderItem;
import com.shop.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepositoryPort orders;
    @Mock ProductCatalogPort catalog;
    @Mock PaymentRequestPort paymentRequests;

    OrderService service;
    UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, catalog, paymentRequests, new DirectTransaction());
    }

    private PlaceOrderCommand command(String productId, int qty) {
        return new PlaceOrderCommand(customerId, "key-1", List.of(new OrderLine(productId, qty)));
    }

    private Order existingOrder() {
        return Order.place(customerId, "key-1", "USD",
                List.of(new OrderItem("p1", "Keyboard", new BigDecimal("89.00"), 1)));
    }

    @Test
    void place_happyPath_createsPendingOrderAndRequestsPayment() {
        when(orders.findByCustomerAndIdempotencyKey(customerId, "key-1")).thenReturn(Optional.empty());
        when(catalog.findByIds(any(Collection.class)))
                .thenReturn(List.of(new CatalogProduct("p1", "Keyboard", new BigDecimal("89.00"), "USD", 10, true)));

        Order order = service.place(command("p1", 2));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.totalAmount()).isEqualByComparingTo("178.00");
        assertThat(order.paymentAttempts()).isEqualTo(1);
        verify(orders).insert(order);
        verify(paymentRequests).requestPayment(order);
    }

    @Test
    void place_sameIdempotencyKey_returnsExistingOrder_withoutSideEffects() {
        Order existing = existingOrder();
        when(orders.findByCustomerAndIdempotencyKey(customerId, "key-1")).thenReturn(Optional.of(existing));

        Order result = service.place(command("p1", 1));

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(catalog, paymentRequests);
        verify(orders, never()).insert(any());
    }

    @Test
    void place_concurrentDuplicate_returnsTheWinnerOrder() {
        Order winner = existingOrder();
        when(orders.findByCustomerAndIdempotencyKey(customerId, "key-1"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(catalog.findByIds(any(Collection.class)))
                .thenReturn(List.of(new CatalogProduct("p1", "Keyboard", new BigDecimal("89.00"), "USD", 10, true)));
        doThrow(new DuplicateOrderException("key-1", null)).when(orders).insert(any());

        Order result = service.place(command("p1", 1));

        assertThat(result).isSameAs(winner);
    }

    @Test
    void place_insufficientStock_isRejected_andNothingIsWritten() {
        when(orders.findByCustomerAndIdempotencyKey(customerId, "key-1")).thenReturn(Optional.empty());
        when(catalog.findByIds(any(Collection.class)))
                .thenReturn(List.of(new CatalogProduct("p1", "Keyboard", new BigDecimal("89.00"), "USD", 1, true)));

        assertThatThrownBy(() -> service.place(command("p1", 2))).isInstanceOf(InsufficientStockException.class);
        verify(orders, never()).insert(any());
        verifyNoInteractions(paymentRequests);
    }

    @Test
    void place_unknownProduct_isRejected() {
        when(orders.findByCustomerAndIdempotencyKey(customerId, "key-1")).thenReturn(Optional.empty());
        when(catalog.findByIds(any(Collection.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.place(command("nope", 1))).isInstanceOf(ProductNotFoundException.class);
    }
}
