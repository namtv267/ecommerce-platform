package com.shop.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Transactional Outbox - phía ghi. PHẢI gọi bên trong transaction của nghiệp vụ,
 * để "đổi trạng thái DB" và "ghi message cần gửi" cùng commit hoặc cùng rollback.
 */
public class OutboxWriter {

    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    public OutboxWriter(JdbcClient jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    /** @return id của dòng outbox, cũng chính là header message-id khi gửi lên Kafka. */
    public UUID append(String topic, String key, String type, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("OutboxWriter.append must run inside a transaction");
        }
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize outbox payload " + type, e);
        }
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO outbox_events (id, topic, message_key, message_type, payload, created_at)
                VALUES (:id, :topic, :key, :type, :payload, now())
                """)
                .param("id", id)
                .param("topic", topic)
                .param("key", key)
                .param("type", type)
                .param("payload", json)
                .update();
        return id;
    }
}
