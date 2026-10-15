package com.shop.auth.application.port.in;

public interface RefreshTokenUseCase {

    /** Refresh token rotation: token cũ bị hủy ngay khi dùng, trả về cặp token mới. */
    AuthTokens refresh(String refreshToken);
}
