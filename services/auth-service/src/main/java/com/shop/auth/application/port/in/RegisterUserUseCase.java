package com.shop.auth.application.port.in;

import com.shop.auth.domain.model.Role;

import java.util.UUID;

public interface RegisterUserUseCase {

    RegisteredUser register(RegisterUserCommand command);

    record RegisterUserCommand(String email, String rawPassword, String fullName) {}

    record RegisteredUser(UUID id, String email, String fullName, Role role) {}
}
