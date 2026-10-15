package com.shop.messaging;

import com.shop.contracts.MsgHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

public final class MessageHeaderUtils {
    private MessageHeaderUtils() {}

    public static String header(ConsumerRecord<?, ?> record, String name) {
        Header h = record.headers().lastHeader(name);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    /** Ưu tiên message-id do outbox gán; nếu thiếu thì dùng toạ độ topic-partition-offset. */
    public static String messageId(ConsumerRecord<?, ?> record) {
        String id = header(record, MsgHeaders.MESSAGE_ID);
        return id != null ? id : record.topic() + "-" + record.partition() + "-" + record.offset();
    }

    public static String messageType(ConsumerRecord<?, ?> record) {
        return header(record, MsgHeaders.MESSAGE_TYPE);
    }
}
