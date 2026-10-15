package com.shop.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.contracts.Topics;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"})
@EnableScheduling
public class MessagingAutoConfiguration {

    @Bean
    public OutboxWriter outboxWriter(JdbcClient jdbc, ObjectMapper mapper) {
        return new OutboxWriter(jdbc, mapper);
    }

    @Bean
    public ProcessedMessageStore processedMessageStore(JdbcClient jdbc) {
        return new ProcessedMessageStore(jdbc);
    }

    @Bean
    public OutboxRelay outboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka,
                                   PlatformTransactionManager txManager,
                                   @Value("${app.outbox.batch-size:100}") int batchSize) {
        return new OutboxRelay(jdbc, kafka, txManager, batchSize, Duration.ofSeconds(5));
    }

    /**
     * Consumer lỗi: retry 3 lần (cách 1s) rồi đẩy sang topic "<topic>.DLT" để không chặn partition.
     * Spring Boot tự gắn bean CommonErrorHandler này vào listener container factory.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafka,
                (record, ex) -> new TopicPartition(record.topic() + Topics.DLT_SUFFIX, -1));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    @Bean
    public KafkaAdmin.NewTopics sagaTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(Topics.PAYMENT_COMMANDS).partitions(3).replicas(1).build(),
                TopicBuilder.name(Topics.PAYMENT_EVENTS).partitions(3).replicas(1).build(),
                TopicBuilder.name(Topics.PAYMENT_COMMANDS + Topics.DLT_SUFFIX).partitions(1).replicas(1).build(),
                TopicBuilder.name(Topics.PAYMENT_EVENTS + Topics.DLT_SUFFIX).partitions(1).replicas(1).build());
    }
}
