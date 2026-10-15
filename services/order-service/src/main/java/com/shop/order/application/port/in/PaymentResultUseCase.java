package com.shop.order.application.port.in;

import com.shop.contracts.PaymentCompletedEvent;
import com.shop.contracts.PaymentFailedEvent;

/** Các bước phản ứng của saga khi payment-service trả kết quả. */
public interface PaymentResultUseCase {
    void onPaymentCompleted(String messageId, PaymentCompletedEvent event);

    void onPaymentFailed(String messageId, PaymentFailedEvent event);
}
