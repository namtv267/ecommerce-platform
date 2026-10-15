package com.shop.order.adapter.in.web;

import com.shop.order.adapter.in.web.dto.OrderResponse;
import com.shop.order.adapter.in.web.dto.PlaceOrderRequest;
import com.shop.order.application.port.in.GetOrderUseCase;
import com.shop.order.application.port.in.PlaceOrderUseCase;
import com.shop.order.application.port.in.PlaceOrderUseCase.OrderLine;
import com.shop.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.shop.order.domain.model.Order;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final GetOrderUseCase getOrder;
    private final IdempotencyExecutor idempotency;

    OrderController(PlaceOrderUseCase placeOrder, GetOrderUseCase getOrder, IdempotencyExecutor idempotency) {
        this.placeOrder = placeOrder;
        this.getOrder = getOrder;
        this.idempotency = idempotency;
    }

    /** 202 Accepted: đơn đã tạo (PENDING), thanh toán xử lý bất đồng bộ qua saga. Poll GET để xem kết quả. */
    @PostMapping
    ResponseEntity<?> place(@AuthenticationPrincipal Jwt jwt,
                            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
                            @Valid @RequestBody PlaceOrderRequest request) {
        String idempotencyKey = IdempotencyExecutor.requireValidKey(idempotencyKeyHeader);
        UUID customerId = UUID.fromString(jwt.getSubject());

        return idempotency.execute(customerId.toString(), idempotencyKey, RequestFingerprint.of(request), () -> {
            Order order = placeOrder.place(new PlaceOrderCommand(customerId, idempotencyKey,
                    request.items().stream().map(i -> new OrderLine(i.productId(), i.quantity())).toList()));
            return ResponseEntity.accepted()
                    .location(URI.create("/api/orders/" + order.id()))
                    .body(OrderResponse.from(order));
        });
    }

    @GetMapping("/{id}")
    OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return OrderResponse.from(getOrder.get(id, UUID.fromString(jwt.getSubject())));
    }
}
