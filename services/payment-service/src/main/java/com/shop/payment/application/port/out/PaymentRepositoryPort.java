package com.shop.payment.application.port.out;

import com.shop.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByOrderId(UUID orderId);

    /** @throws com.shop.payment.domain.exception.DuplicatePaymentException khi trùng idempotency_key / order_id */
    Payment insert(Payment payment);

    /** UPDATE ... WHERE status = 'PENDING': chỉ 1 bên "thắng" khi quyết định kết quả. @return true nếu đã cập nhật. */
    boolean updateIfPending(Payment payment);
}
