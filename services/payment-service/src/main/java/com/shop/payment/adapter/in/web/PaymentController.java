package com.shop.payment.adapter.in.web;

import com.shop.payment.adapter.in.web.dto.PaymentResponse;
import com.shop.payment.application.port.in.GetPaymentUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Chỉ đọc. Thanh toán được kích hoạt bằng message Kafka, không có API tạo thanh toán trực tiếp. */
@RestController
@RequestMapping("/api/payments")
class PaymentController {

    private final GetPaymentUseCase getPayment;

    PaymentController(GetPaymentUseCase getPayment) {
        this.getPayment = getPayment;
    }

    @GetMapping("/order/{orderId}")
    PaymentResponse byOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return PaymentResponse.from(getPayment.getByOrderId(orderId, UUID.fromString(jwt.getSubject())));
    }
}
