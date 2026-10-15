package com.shop.order.application.port.in;

import com.shop.order.domain.model.Order;

import java.util.List;
import java.util.UUID;

public interface PlaceOrderUseCase {

    Order place(PlaceOrderCommand command);

    record PlaceOrderCommand(UUID customerId, String idempotencyKey, List<OrderLine> lines) {}

    record OrderLine(String productId, int quantity) {}
}
