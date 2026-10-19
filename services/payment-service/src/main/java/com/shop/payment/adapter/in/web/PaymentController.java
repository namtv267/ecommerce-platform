package com.shop.payment.adapter.in.web;

import com.shop.payment.adapter.in.web.dto.PaymentResponse;
import com.shop.payment.application.port.in.GetPaymentUseCase;
import com.shop.payment.application.service.ProcessPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
class PaymentController {

    private final GetPaymentUseCase getPayment;
    private final ProcessPaymentService processPayment;

    PaymentController(GetPaymentUseCase getPayment, ProcessPaymentService processPayment) {
        this.getPayment = getPayment;
        this.processPayment = processPayment;
    }

    @GetMapping("/order/{orderId}")
    PaymentResponse byOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        if (jwt != null) {
            return PaymentResponse.from(getPayment.getByOrderId(orderId, UUID.fromString(jwt.getSubject())));
        }
        return PaymentResponse.from(processPayment.getByOrderId(orderId));
    }

    @PostMapping("/approve/{orderId}")
    PaymentResponse approve(@PathVariable UUID orderId) {
        return PaymentResponse.from(processPayment.approve(orderId));
    }

    @GetMapping("/approve/{orderId}")
    ResponseEntity<?> approveAndRedirect(@PathVariable UUID orderId) {
        processPayment.approve(orderId);
        return ResponseEntity.status(302)
                .location(URI.create("/payment-confirm?orderId=" + orderId))
                .build();
    }
}
