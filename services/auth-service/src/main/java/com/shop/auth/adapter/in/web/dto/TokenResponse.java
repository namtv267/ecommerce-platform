package com.shop.auth.adapter.in.web.dto;

import com.shop.auth.application.port.in.AuthTokens;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    public static TokenResponse from(AuthTokens t) {
        return new TokenResponse(t.accessToken(), t.refreshToken(), t.tokenType(), t.expiresInSeconds());
    }
}
