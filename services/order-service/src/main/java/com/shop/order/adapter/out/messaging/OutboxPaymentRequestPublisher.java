package com.shop.order.adapter.out.messaging;

import com.shop.contracts.MessageTypes;
import com.shop.contracts.ProcessPaymentCommand;
import com.shop.contracts.Topics;
import com.shop.messaging.OutboxWriter;
import com.shop.order.application.port.out.PaymentRequestPort;
import com.shop.order.domain.model.Order;
import org.springframework.stereotype.Component;

@Component
class OutboxPaymentRequestPublisher implements PaymentRequestPort {

    private final OutboxWriter outbox;

    OutboxPaymentRequestPublisher(OutboxWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void requestPayment(Order order) {
        ProcessPaymentCommand command = new ProcessPaymentCommand(order.id(), order.customerId(),
                order.totalAmount(), order.currency(), order.paymentIdempotencyKey());
        // key = orderId -> mọi message của 1 đơn vào cùng partition, được xử lý tuần tự
        outbox.append(Topics.PAYMENT_COMMANDS, order.id().toString(), MessageTypes.PROCESS_PAYMENT, command);
    }
}
