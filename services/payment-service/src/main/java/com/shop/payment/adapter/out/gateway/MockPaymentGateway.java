package com.shop.payment.adapter.out.gateway;

import com.shop.payment.application.port.out.PaymentGatewayPort;
import com.shop.payment.domain.exception.GatewayTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cổng thanh toán giả lập, idempotent theo idempotencyKey như Stripe/VNPay thật.
 * Kịch bản xác định theo phần xu (cents) của số tiền, để demo/test không phụ thuộc may rủi:
 *   .13 -> bị từ chối (INSUFFICIENT_FUNDS)
 *   .14 -> ĐÃ trừ tiền nhưng mất phản hồi (timeout) ở lần gọi đầu tiên; gọi lại cùng key trả về kết quả cũ
 * Ngoài ra có thể cấu hình tỉ lệ từ chối / mất request ngẫu nhiên.
 */
@Component
public class MockPaymentGateway implements PaymentGatewayPort {

    private final double failureRate;
    private final double timeoutRate;
    private final long latencyMs;

    private final ConcurrentMap<String, GatewayResult> processed = new ConcurrentHashMap<>();
    private final Set<String> timeoutAlreadyTriggered = ConcurrentHashMap.newKeySet();
    private final AtomicInteger realCharges = new AtomicInteger();

    public MockPaymentGateway(@Value("${app.payment.gateway.failure-rate:0.0}") double failureRate,
                              @Value("${app.payment.gateway.timeout-rate:0.0}") double timeoutRate,
                              @Value("${app.payment.gateway.latency-ms:300}") long latencyMs) {
        this.failureRate = failureRate;
        this.timeoutRate = timeoutRate;
        this.latencyMs = latencyMs;
    }

    @Override
    public GatewayResult charge(GatewayChargeRequest request) {
        sleep();
        int cents = request.amount().remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();

        // Request bị mất trên đường truyền: cổng chưa nhận được gì
        if (timeoutRate > 0 && ThreadLocalRandom.current().nextDouble() < timeoutRate) {
            throw new GatewayTimeoutException("Gateway timeout (request lost)");
        }

        // Idempotency của cổng: cùng key chỉ quyết định đúng 1 lần
        GatewayResult result = processed.computeIfAbsent(request.idempotencyKey(), key -> decide(cents));

        // Phản hồi bị mất sau khi cổng đã xử lý xong
        if (cents == 14 && result.approved() && timeoutAlreadyTriggered.add(request.idempotencyKey())) {
            throw new GatewayTimeoutException("Gateway timeout (charged, response lost)");
        }
        return result;
    }

    /** Số lần "trừ tiền thật" - dùng trong test để chứng minh không bao giờ trừ 2 lần. */
    public int chargeCount() {
        return realCharges.get();
    }

    private GatewayResult decide(int cents) {
        if (cents == 13) {
            return GatewayResult.declined("INSUFFICIENT_FUNDS");
        }
        if (failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
            return GatewayResult.declined("CARD_DECLINED");
        }
        realCharges.incrementAndGet();
        return GatewayResult.approved("gw_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
    }

    private void sleep() {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
