package com.shop.messaging;

import com.shop.contracts.MsgHeaders;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Transactional Outbox - phía gửi: poll các dòng chưa publish rồi đẩy lên Kafka.
 * Gửi xong mới đánh dấu published -> at-least-once (consumer phải idempotent).
 * Lưu ý: chạy nhiều instance với SKIP LOCKED có thể làm lệch thứ tự theo key; local chỉ chạy 1 instance.
 */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private record OutboxRow(UUID id, String topic, String key, String type, String payload) {}

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;
    private final int batchSize;
    private final Duration sendTimeout;

    public OutboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka,
                       PlatformTransactionManager txManager, int batchSize, Duration sendTimeout) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.tx = new TransactionTemplate(txManager);
        this.batchSize = batchSize;
        this.sendTimeout = sendTimeout;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:500}")
    public void poll() {
        try {
            tx.executeWithoutResult(status -> publishBatch());
        } catch (Exception ex) {
            log.warn("Outbox poll failed, will retry: {}", ex.toString());
        }
    }

    private void publishBatch() {
        List<OutboxRow> rows = jdbc.sql("""
                SELECT id, topic, message_key, message_type, payload
                FROM outbox_events
                WHERE published_at IS NULL
                ORDER BY created_at, id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """)
                .param("limit", batchSize)
                .query((rs, i) -> new OutboxRow(rs.getObject("id", UUID.class), rs.getString("topic"),
                        rs.getString("message_key"), rs.getString("message_type"), rs.getString("payload")))
                .list();

        for (OutboxRow row : rows) {
            ProducerRecord<String, String> record = new ProducerRecord<>(row.topic(), row.key(), row.payload());
            record.headers().add(MsgHeaders.MESSAGE_ID, row.id().toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add(MsgHeaders.MESSAGE_TYPE, row.type().getBytes(StandardCharsets.UTF_8));
            try {
                kafka.send(record).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException | TimeoutException e) {
                log.warn("Cannot publish outbox {} to {}: {}", row.id(), row.topic(), e.toString());
                break; // giữ nguyên thứ tự: dừng batch, các dòng đã gửi vẫn được commit published
            }
            jdbc.sql("UPDATE outbox_events SET published_at = now() WHERE id = :id")
                    .param("id", row.id())
                    .update();
        }
    }
}
