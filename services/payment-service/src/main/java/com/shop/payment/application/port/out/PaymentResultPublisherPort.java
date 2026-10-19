package com.shop.payment.application.port.out;

import com.shop.payment.domain.model.Payment;

public interface PaymentResultPublisherPort {
    /** Ghi PaymentCompleted/PaymentFailed vào outbox. Phải gọi trong transaction đang mở. */
    void publish(Payment payment);
}
