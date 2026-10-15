package com.shop.auth.application.port.in;

public interface LoginUseCase {

    AuthTokens login(LoginCommand command);

    record LoginCommand(String email, String rawPassword) {}
}
