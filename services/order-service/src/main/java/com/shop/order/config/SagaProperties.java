package com.shop.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.saga")
public record SagaProperties(Duration paymentTimeout, int maxPaymentAttempts) {
}
