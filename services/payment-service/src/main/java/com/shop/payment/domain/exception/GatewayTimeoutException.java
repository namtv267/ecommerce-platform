package com.shop.payment.domain.exception;

/** Không biết kết quả ở cổng thanh toán (có thể ĐÃ trừ tiền). An toàn để retry NHỜ idempotency key. */
public class GatewayTimeoutException extends RuntimeException {
    public GatewayTimeoutException(String message) {
        super(message);
    }
}
