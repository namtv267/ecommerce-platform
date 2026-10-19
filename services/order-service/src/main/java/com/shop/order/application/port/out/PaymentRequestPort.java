package com.shop.order.application.port.out;

import com.shop.order.domain.model.Order;

public interface PaymentRequestPort {
    /** Ghi lệnh ProcessPayment vào outbox. Phải gọi trong transaction đang mở. */
    void requestPayment(Order order);
}
