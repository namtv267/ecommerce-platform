package com.shop.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtVerificationProperties(String secret, String issuer) {
}
