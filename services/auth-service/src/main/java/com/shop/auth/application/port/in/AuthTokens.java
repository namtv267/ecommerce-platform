package com.shop.auth.application.port.in;

public record AuthTokens(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
    public static AuthTokens bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthTokens(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
