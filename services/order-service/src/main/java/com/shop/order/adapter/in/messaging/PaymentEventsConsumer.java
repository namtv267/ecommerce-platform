package com.shop.order.adapter.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.contracts.MessageTypes;
import com.shop.contracts.PaymentCompletedEvent;
import com.shop.contracts.PaymentFailedEvent;
import com.shop.contracts.Topics;
import com.shop.messaging.MessageHeaderUtils;
import com.shop.order.application.port.in.PaymentResultUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Lỗi ném ra từ đây sẽ được DefaultErrorHandler retry 3 lần rồi đẩy sang payment.events.DLT.
 */
@Component
class PaymentEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    private final PaymentResultUseCase useCase;
    private final ObjectMapper mapper;

    PaymentEventsConsumer(PaymentResultUseCase useCase, ObjectMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS)
    void onMessage(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String messageId = MessageHeaderUtils.messageId(record);
        String type = MessageHeaderUtils.messageType(record);
        if (type == null) {
            log.warn("Message {} has no message-type header, skipped", messageId);
            return;
        }
        switch (type) {
            case MessageTypes.PAYMENT_COMPLETED ->
                    useCase.onPaymentCompleted(messageId, mapper.readValue(record.value(), PaymentCompletedEvent.class));
            case MessageTypes.PAYMENT_FAILED ->
                    useCase.onPaymentFailed(messageId, mapper.readValue(record.value(), PaymentFailedEvent.class));
            default -> log.warn("Unknown message type {} on {}, skipped", type, record.topic());
        }
    }
}
