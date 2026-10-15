package com.shop.payment.application.port.in;

import com.shop.payment.domain.model.Payment;

import java.util.UUID;

public interface GetPaymentUseCase {
    Payment getByOrderId(UUID orderId, UUID customerId);
}
