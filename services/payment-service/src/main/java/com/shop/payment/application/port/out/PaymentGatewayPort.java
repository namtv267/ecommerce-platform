package com.shop.payment.application.port.out;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    /**
     * Gọi cổng thanh toán. Cổng PHẢI idempotent theo {@code idempotencyKey}:
     * gọi lại với cùng key trả về đúng kết quả cũ, không trừ tiền lần nữa.
     * @throws com.shop.payment.domain.exception.GatewayTimeoutException khi không nhận được phản hồi
     */
    GatewayResult charge(GatewayChargeRequest request);

    record GatewayChargeRequest(String idempotencyKey, BigDecimal amount, String currency) {}

    record GatewayResult(boolean approved, String reference, String declineReason) {
        public static GatewayResult approved(String reference) {
            return new GatewayResult(true, reference, null);
        }

        public static GatewayResult declined(String reason) {
            return new GatewayResult(false, null, reason);
        }
    }
}
