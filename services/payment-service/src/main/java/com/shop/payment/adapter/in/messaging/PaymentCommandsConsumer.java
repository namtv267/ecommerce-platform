package com.shop.payment.adapter.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.contracts.MessageTypes;
import com.shop.contracts.ProcessPaymentCommand;
import com.shop.contracts.Topics;
import com.shop.messaging.MessageHeaderUtils;
import com.shop.payment.application.port.in.ProcessPaymentUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Offset chỉ được commit sau khi listener trả về thành công. Lỗi (vd. gateway timeout) -> DefaultErrorHandler
 * retry 3 lần, mỗi lần gọi lại cổng với CÙNG idempotency key -> an toàn. Hết lượt -> payment.commands.DLT.
 */
@Component
class PaymentCommandsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandsConsumer.class);

    private final ProcessPaymentUseCase useCase;
    private final ObjectMapper mapper;

    PaymentCommandsConsumer(ProcessPaymentUseCase useCase, ObjectMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMMANDS)
    void onMessage(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String messageId = MessageHeaderUtils.messageId(record);
        String type = MessageHeaderUtils.messageType(record);
        if (!MessageTypes.PROCESS_PAYMENT.equals(type)) {
            log.warn("Unknown message type {} on {}, skipped", type, record.topic());
            return;
        }
        useCase.process(messageId, mapper.readValue(record.value(), ProcessPaymentCommand.class));
    }
}
