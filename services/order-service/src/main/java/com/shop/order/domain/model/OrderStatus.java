package com.shop.order.domain.model;

/** PENDING -> CONFIRMED (payment OK) hoặc PENDING -> CANCELLED (payment thất bại / quá hạn). */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
