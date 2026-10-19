package com.shop.payment.adapter.out.messaging;

import com.shop.contracts.MessageTypes;
import com.shop.contracts.PaymentCompletedEvent;
import com.shop.contracts.PaymentFailedEvent;
import com.shop.contracts.Topics;
import com.shop.messaging.OutboxWriter;
import com.shop.payment.application.port.out.PaymentResultPublisherPort;
import com.shop.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

@Component
class OutboxPaymentResultPublisher implements PaymentResultPublisherPort {

    private final OutboxWriter outbox;

    OutboxPaymentResultPublisher(OutboxWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void publish(Payment p) {
        String key = p.orderId().toString();
        switch (p.status()) {
            case COMPLETED -> outbox.append(Topics.PAYMENT_EVENTS, key, MessageTypes.PAYMENT_COMPLETED,
                    new PaymentCompletedEvent(p.orderId(), p.id(), p.gatewayReference(), p.updatedAt()));
            case FAILED -> outbox.append(Topics.PAYMENT_EVENTS, key, MessageTypes.PAYMENT_FAILED,
                    new PaymentFailedEvent(p.orderId(), p.id(), p.failureReason(), p.updatedAt()));
            case PENDING -> throw new IllegalStateException("Cannot publish a PENDING payment");
        }
    }
}
