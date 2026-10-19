package com.shop.auth.application.port.out;

import com.shop.auth.domain.model.User;

import java.time.Instant;

public interface AccessTokenIssuerPort {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, String id, Instant expiresAt) {}
}
