package com.shop.auth.application.port.in;

import java.time.Instant;

public interface LogoutUseCase {

    void logout(LogoutCommand command);

    record LogoutCommand(String refreshToken, String accessTokenId, Instant accessTokenExpiresAt) {}
}
