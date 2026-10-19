package com.shop.order.application.port.out;

import com.shop.order.domain.model.Order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    /** @throws com.shop.order.domain.exception.DuplicateOrderException khi trùng (customerId, idempotencyKey) */
    void insert(Order order);

    void update(Order order);

    Optional<Order> findById(UUID id);

    /** SELECT ... FOR UPDATE: khoá dòng để tuần tự hoá với message payment và job timeout. */
    Optional<Order> findByIdForUpdate(UUID id);

    Optional<Order> findByCustomerAndIdempotencyKey(UUID customerId, String idempotencyKey);

    List<UUID> findStalePendingIds(Instant updatedBefore, int limit);
}
