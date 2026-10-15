package com.shop.order.domain.exception;

import com.shop.order.domain.model.OrderStatus;

import java.util.UUID;

public class IllegalOrderStateException extends DomainException {
    public IllegalOrderStateException(UUID id, OrderStatus from, OrderStatus to) {
        super("Order " + id + " cannot move from " + from + " to " + to);
    }
}
