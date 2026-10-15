package com.shop.order.adapter.in.scheduler;

import com.shop.order.application.port.in.PaymentTimeoutUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class PaymentTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutScheduler.class);

    private final PaymentTimeoutUseCase useCase;

    PaymentTimeoutScheduler(PaymentTimeoutUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${app.saga.check-interval-ms:15000}")
    void run() {
        try {
            int handled = useCase.handleStalePayments();
            if (handled > 0) {
                log.info("Handled {} stale pending order(s)", handled);
            }
        } catch (Exception ex) {
            log.warn("Stale payment check failed: {}", ex.toString());
        }
    }
}
