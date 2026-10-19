package com.shop.payment.adapter.out.persistence;

import com.shop.payment.application.port.out.PaymentRepositoryPort;
import com.shop.payment.domain.exception.DuplicatePaymentException;
import com.shop.payment.domain.model.Payment;
import com.shop.payment.domain.model.PaymentStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Component
class JdbcPaymentRepository implements PaymentRepositoryPort {

    private static final RowMapper<Payment> MAPPER = (rs, i) -> Payment.reconstitute(
            rs.getObject("id", UUID.class),
            rs.getObject("order_id", UUID.class),
            rs.getObject("customer_id", UUID.class),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            rs.getString("idempotency_key"),
            PaymentStatus.valueOf(rs.getString("status")),
            rs.getString("gateway_reference"),
            rs.getString("failure_reason"),
            instant(rs, "created_at"),
            instant(rs, "updated_at"));

    private final JdbcClient jdbc;

    JdbcPaymentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jdbc.sql("SELECT * FROM payments WHERE idempotency_key = :key")
                .param("key", idempotencyKey)
                .query(MAPPER)
                .optional();
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jdbc.sql("SELECT * FROM payments WHERE order_id = :orderId")
                .param("orderId", orderId)
                .query(MAPPER)
                .optional();
    }

    @Override
    public Payment insert(Payment p) {
        try {
            jdbc.sql("""
                    INSERT INTO payments (id, order_id, customer_id, amount, currency, idempotency_key, status,
                                          gateway_reference, failure_reason, created_at, updated_at)
                    VALUES (:id, :orderId, :customerId, :amount, :currency, :key, :status,
                            :ref, :reason, :createdAt, :updatedAt)
                    """)
                    .param("id", p.id())
                    .param("orderId", p.orderId())
                    .param("customerId", p.customerId())
                    .param("amount", p.amount())
                    .param("currency", p.currency())
                    .param("key", p.idempotencyKey())
                    .param("status", p.status().name())
                    .param("ref", p.gatewayReference())
                    .param("reason", p.failureReason())
                    .param("createdAt", odt(p.createdAt()))
                    .param("updatedAt", odt(p.updatedAt()))
                    .update();
            return p;
        } catch (DuplicateKeyException e) {
            throw new DuplicatePaymentException(p.idempotencyKey(), e);
        }
    }

    @Override
    public boolean updateIfPending(Payment p) {
        return jdbc.sql("""
                UPDATE payments
                SET status = :status, gateway_reference = :ref, failure_reason = :reason, updated_at = :updatedAt
                WHERE id = :id AND status = 'PENDING'
                """)
                .param("status", p.status().name())
                .param("ref", p.gatewayReference())
                .param("reason", p.failureReason())
                .param("updatedAt", odt(p.updatedAt()))
                .param("id", p.id())
                .update() == 1;
    }

    private static OffsetDateTime odt(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }
}
