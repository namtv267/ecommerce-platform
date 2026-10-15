package com.shop.contracts;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lệnh yêu cầu thanh toán. {@code idempotencyKey} ổn định theo đơn hàng
 * (gửi lại bao nhiêu lần cũng cùng key) -> payment-service đảm bảo chỉ trừ tiền 1 lần.
 */
public record ProcessPaymentCommand(UUID orderId, UUID customerId, BigDecimal amount,
                                    String currency, String idempotencyKey) {
}
