package com.shop.auth.adapter.out.persistence;

import com.shop.auth.application.port.out.UserRepositoryPort;
import com.shop.auth.domain.exception.EmailAlreadyUsedException;
import com.shop.auth.domain.model.Email;
import com.shop.auth.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;

    UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        try {
            return toDomain(repository.saveAndFlush(toEntity(user)));
        } catch (DataIntegrityViolationException ex) {
            // Hai request đăng ký cùng email chạy song song: unique constraint là chốt chặn cuối cùng
            throw new EmailAlreadyUsedException(user.email().value());
        }
    }

    private UserJpaEntity toEntity(User u) {
        return new UserJpaEntity(u.id(), u.email().value(), u.passwordHash(), u.fullName(), u.role(), u.createdAt());
    }

    private User toDomain(UserJpaEntity e) {
        return User.reconstitute(e.getId(), new Email(e.getEmail()), e.getPasswordHash(),
                e.getFullName(), e.getRole(), e.getCreatedAt());
    }
}
