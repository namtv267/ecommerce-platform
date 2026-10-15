package com.shop.payment.domain.exception;

import com.shop.payment.domain.model.PaymentStatus;

import java.util.UUID;

public class IllegalPaymentStateException extends RuntimeException {
    public IllegalPaymentStateException(UUID id, PaymentStatus from, PaymentStatus to) {
        super("Payment " + id + " cannot move from " + from + " to " + to);
    }
}
