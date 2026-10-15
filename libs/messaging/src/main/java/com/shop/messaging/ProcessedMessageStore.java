package com.shop.messaging;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Inbox / khử trùng lặp phía consumer: Kafka là at-least-once nên cùng 1 message có thể đến nhiều lần. */
public class ProcessedMessageStore {

    private final JdbcClient jdbc;

    public ProcessedMessageStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Đánh dấu đã xử lý. Gọi trong CÙNG transaction với thay đổi nghiệp vụ.
     * @return true nếu là lần đầu; false nếu message đã được xử lý trước đó.
     */
    public boolean markProcessed(String messageId) {
        return jdbc.sql("""
                INSERT INTO processed_messages (message_id, processed_at) VALUES (:id, now())
                ON CONFLICT (message_id) DO NOTHING
                """)
                .param("id", messageId)
                .update() == 1;
    }

    public boolean isProcessed(String messageId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM processed_messages WHERE message_id = :id)")
                .param("id", messageId)
                .query(Boolean.class)
                .single();
    }
}
