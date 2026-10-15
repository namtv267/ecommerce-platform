package com.shop.payment.application.port.in;

import com.shop.contracts.ProcessPaymentCommand;

public interface ProcessPaymentUseCase {
    /**
     * Xử lý lệnh thanh toán, an toàn khi gọi lặp (cùng messageId hoặc cùng idempotencyKey).
     * @throws com.shop.payment.domain.exception.GatewayTimeoutException nếu chưa biết kết quả -> cần retry
     */
    void process(String messageId, ProcessPaymentCommand command);
}
