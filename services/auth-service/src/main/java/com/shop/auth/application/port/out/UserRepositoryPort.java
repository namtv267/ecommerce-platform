package com.shop.auth.application.port.out;

import com.shop.auth.domain.model.Email;
import com.shop.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<User> findByEmail(Email email);

    Optional<User> findById(UUID id);

    /** @throws com.shop.auth.domain.exception.EmailAlreadyUsedException khi vi phạm unique email */
    User save(User user);
}
