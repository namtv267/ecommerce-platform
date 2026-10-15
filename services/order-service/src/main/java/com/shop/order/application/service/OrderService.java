package com.shop.order.application.service;

import com.shop.order.application.port.in.GetOrderUseCase;
import com.shop.order.application.port.in.PlaceOrderUseCase;
import com.shop.order.application.port.out.OrderRepositoryPort;
import com.shop.order.application.port.out.PaymentRequestPort;
import com.shop.order.application.port.out.ProductCatalogPort;
import com.shop.order.application.port.out.ProductCatalogPort.CatalogProduct;
import com.shop.order.application.port.out.TransactionPort;
import com.shop.order.domain.exception.CurrencyMismatchException;
import com.shop.order.domain.exception.DuplicateOrderException;
import com.shop.order.domain.exception.InsufficientStockException;
import com.shop.order.domain.exception.OrderNotFoundException;
import com.shop.order.domain.exception.ProductNotFoundException;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Bước đầu của saga: tạo đơn PENDING và yêu cầu thanh toán, cả hai trong 1 transaction (outbox). */
public class OrderService implements PlaceOrderUseCase, GetOrderUseCase {

    private final OrderRepositoryPort orders;
    private final ProductCatalogPort catalog;
    private final PaymentRequestPort paymentRequests;
    private final TransactionPort tx;

    public OrderService(OrderRepositoryPort orders, ProductCatalogPort catalog,
                        PaymentRequestPort paymentRequests, TransactionPort tx) {
        this.orders = orders;
        this.catalog = catalog;
        this.paymentRequests = paymentRequests;
        this.tx = tx;
    }

    @Override
    public Order place(PlaceOrderCommand command) {
        // Idempotency tầng DB (1): key đã dùng -> trả lại đơn cũ, không tạo đơn mới
        Optional<Order> existing = orders.findByCustomerAndIdempotencyKey(command.customerId(), command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        Order order = buildOrder(command);

        try {
            tx.run(() -> {
                orders.insert(order);                   // unique (customer_id, idempotency_key)
                paymentRequests.requestPayment(order);  // outbox: cùng commit với đơn hàng
            });
        } catch (DuplicateOrderException race) {
            // Idempotency tầng DB (2): hai request cùng key chạy song song, request kia thắng
            return orders.findByCustomerAndIdempotencyKey(command.customerId(), command.idempotencyKey())
                    .orElseThrow(() -> race);
        }
        return order;
    }

    @Override
    public Order get(UUID orderId, UUID customerId) {
        return orders.findById(orderId)
                .filter(o -> o.customerId().equals(customerId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private Order buildOrder(PlaceOrderCommand command) {
        // Gộp các dòng trùng productId
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderLine line : command.lines()) {
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }

        Map<String, CatalogProduct> products = catalog.findByIds(quantities.keySet()).stream()
                .collect(Collectors.toMap(CatalogProduct::id, Function.identity()));

        List<OrderItem> items = new ArrayList<>();
        String currency = null;
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            CatalogProduct product = products.get(entry.getKey());
            if (product == null || !product.active()) {
                throw new ProductNotFoundException(entry.getKey());
            }
            if (product.stock() < entry.getValue()) {
                throw new InsufficientStockException(product.id(), entry.getValue(), product.stock());
            }
            if (currency == null) {
                currency = product.currency();
            } else if (!currency.equals(product.currency())) {
                throw new CurrencyMismatchException();
            }
            items.add(new OrderItem(product.id(), product.name(), product.price(), entry.getValue()));
        }
        return Order.place(command.customerId(), command.idempotencyKey(), currency, items);
    }
}
