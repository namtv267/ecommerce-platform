package com.shop.auth.application.service;

import com.shop.auth.application.port.in.AuthTokens;
import com.shop.auth.application.port.in.LoginUseCase;
import com.shop.auth.application.port.in.LogoutUseCase;
import com.shop.auth.application.port.in.RefreshTokenUseCase;
import com.shop.auth.application.port.in.RegisterUserUseCase;
import com.shop.auth.application.port.out.AccessTokenIssuerPort;
import com.shop.auth.application.port.out.AccessTokenIssuerPort.IssuedAccessToken;
import com.shop.auth.application.port.out.PasswordHasherPort;
import com.shop.auth.application.port.out.RefreshTokenStorePort;
import com.shop.auth.application.port.out.TokenBlacklistPort;
import com.shop.auth.application.port.out.UserRepositoryPort;
import com.shop.auth.domain.exception.EmailAlreadyUsedException;
import com.shop.auth.domain.exception.InvalidCredentialsException;
import com.shop.auth.domain.exception.InvalidRefreshTokenException;
import com.shop.auth.domain.model.Email;
import com.shop.auth.domain.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Application service: điều phối use case, KHÔNG biết gì về Spring/JPA/Redis.
 * Được khai báo bean trong config/ApplicationConfig.
 */
public class AuthService implements RegisterUserUseCase, LoginUseCase, RefreshTokenUseCase, LogoutUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final AccessTokenIssuerPort accessTokens;
    private final RefreshTokenStorePort refreshTokens;
    private final TokenBlacklistPort blacklist;

    public AuthService(UserRepositoryPort users, PasswordHasherPort hasher, AccessTokenIssuerPort accessTokens,
                       RefreshTokenStorePort refreshTokens, TokenBlacklistPort blacklist) {
        this.users = users;
        this.hasher = hasher;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.blacklist = blacklist;
    }

    @Override
    public RegisteredUser register(RegisterUserCommand command) {
        Email email = new Email(command.email());
        if (users.findByEmail(email).isPresent()) {
            throw new EmailAlreadyUsedException(email.value());
        }
        User saved = users.save(User.register(email, hasher.hash(command.rawPassword()), command.fullName()));
        return new RegisteredUser(saved.id(), saved.email().value(), saved.fullName(), saved.role());
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        User user = users.findByEmail(new Email(command.email()))
                .filter(u -> hasher.matches(command.rawPassword(), u.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new); // cùng 1 lỗi cho "sai email" và "sai mật khẩu"
        return issueTokens(user);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        UUID userId = refreshTokens.consume(refreshToken).orElseThrow(InvalidRefreshTokenException::new);
        User user = users.findById(userId).orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(user);
    }

    @Override
    public void logout(LogoutCommand command) {
        refreshTokens.revoke(command.refreshToken());
        blacklist.blacklist(command.accessTokenId(), command.accessTokenExpiresAt());
    }

    private AuthTokens issueTokens(User user) {
        IssuedAccessToken access = accessTokens.issue(user);
        String refresh = refreshTokens.create(user.id());
        long expiresIn = Math.max(0, Duration.between(Instant.now(), access.expiresAt()).toSeconds());
        return AuthTokens.bearer(access.value(), refresh, expiresIn);
    }
}
