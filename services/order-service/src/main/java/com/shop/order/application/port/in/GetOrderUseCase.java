package com.shop.order.application.port.in;

import com.shop.order.domain.model.Order;

import java.util.UUID;

public interface GetOrderUseCase {
    /** Chỉ chủ đơn mới xem được; người khác nhận NotFound (không lộ sự tồn tại của đơn). */
    Order get(UUID orderId, UUID customerId);
}
