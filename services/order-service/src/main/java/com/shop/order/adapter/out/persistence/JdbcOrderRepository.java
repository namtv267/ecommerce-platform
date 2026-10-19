package com.shop.order.adapter.out.persistence;

import com.shop.order.application.port.out.OrderRepositoryPort;
import com.shop.order.domain.exception.DuplicateOrderException;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderItem;
import com.shop.order.domain.model.OrderStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class JdbcOrderRepository implements OrderRepositoryPort {

    private record Header(UUID id, UUID customerId, OrderStatus status, String currency, BigDecimal total,
                          String cancelReason, int attempts, String idempotencyKey, Instant createdAt, Instant updatedAt) {}

    private static final RowMapper<Header> HEADER = (rs, i) -> new Header(
            rs.getObject("id", UUID.class),
            rs.getObject("customer_id", UUID.class),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getString("currency"),
            rs.getBigDecimal("total_amount"),
            rs.getString("cancel_reason"),
            rs.getInt("payment_attempts"),
            rs.getString("idempotency_key"),
            instant(rs, "created_at"),
            instant(rs, "updated_at"));

    private final JdbcClient jdbc;

    JdbcOrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(Order o) {
        try {
            jdbc.sql("""
                    INSERT INTO orders (id, customer_id, status, currency, total_amount, cancel_reason,
                                        payment_attempts, idempotency_key, created_at, updated_at)
                    VALUES (:id, :customerId, :status, :currency, :total, :cancelReason,
                            :attempts, :key, :createdAt, :updatedAt)
                    """)
                    .param("id", o.id())
                    .param("customerId", o.customerId())
                    .param("status", o.status().name())
                    .param("currency", o.currency())
                    .param("total", o.totalAmount())
                    .param("cancelReason", o.cancelReason())
                    .param("attempts", o.paymentAttempts())
                    .param("key", o.idempotencyKey())
                    .param("createdAt", odt(o.createdAt()))
                    .param("updatedAt", odt(o.updatedAt()))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateOrderException(o.idempotencyKey(), e);
        }
        for (OrderItem item : o.items()) {
            jdbc.sql("""
                    INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity)
                    VALUES (:orderId, :productId, :name, :price, :qty)
                    """)
                    .param("orderId", o.id())
                    .param("productId", item.productId())
                    .param("name", item.productName())
                    .param("price", item.unitPrice())
                    .param("qty", item.quantity())
                    .update();
        }
    }

    @Override
    public void update(Order o) {
        jdbc.sql("""
                UPDATE orders
                SET status = :status, cancel_reason = :cancelReason, payment_attempts = :attempts, updated_at = :updatedAt
                WHERE id = :id
                """)
                .param("status", o.status().name())
                .param("cancelReason", o.cancelReason())
                .param("attempts", o.paymentAttempts())
                .param("updatedAt", odt(o.updatedAt()))
                .param("id", o.id())
                .update();
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return load(jdbc.sql("SELECT * FROM orders WHERE id = :id").param("id", id).query(HEADER).optional());
    }

    @Override
    public Optional<Order> findByIdForUpdate(UUID id) {
        return load(jdbc.sql("SELECT * FROM orders WHERE id = :id FOR UPDATE").param("id", id).query(HEADER).optional());
    }

    @Override
    public Optional<Order> findByCustomerAndIdempotencyKey(UUID customerId, String idempotencyKey) {
        return load(jdbc.sql("SELECT * FROM orders WHERE customer_id = :customerId AND idempotency_key = :key")
                .param("customerId", customerId)
                .param("key", idempotencyKey)
                .query(HEADER)
                .optional());
    }

    @Override
    public List<UUID> findStalePendingIds(Instant updatedBefore, int limit) {
        return jdbc.sql("""
                SELECT id FROM orders
                WHERE status = 'PENDING' AND updated_at < :before
                ORDER BY updated_at
                LIMIT :limit
                """)
                .param("before", odt(updatedBefore))
                .param("limit", limit)
                .query(UUID.class)
                .list();
    }

    private Optional<Order> load(Optional<Header> header) {
        return header.map(h -> Order.reconstitute(h.id(), h.customerId(), h.status(), h.currency(), h.total(),
                h.cancelReason(), h.attempts(), h.idempotencyKey(), items(h.id()), h.createdAt(), h.updatedAt()));
    }

    private List<OrderItem> items(UUID orderId) {
        return jdbc.sql("""
                SELECT product_id, product_name, unit_price, quantity
                FROM order_items WHERE order_id = :id ORDER BY id
                """)
                .param("id", orderId)
                .query((rs, i) -> new OrderItem(rs.getString("product_id"), rs.getString("product_name"),
                        rs.getBigDecimal("unit_price"), rs.getInt("quantity")))
                .list();
    }

    /** pgjdbc không nhận java.time.Instant trực tiếp -> đổi sang OffsetDateTime (UTC). */
    private static OffsetDateTime odt(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }
}
