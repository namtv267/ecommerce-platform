package com.shop.auth.application;

import com.shop.auth.application.port.in.AuthTokens;
import com.shop.auth.application.port.in.LoginUseCase.LoginCommand;
import com.shop.auth.application.port.in.LogoutUseCase.LogoutCommand;
import com.shop.auth.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.shop.auth.application.port.out.AccessTokenIssuerPort;
import com.shop.auth.application.port.out.AccessTokenIssuerPort.IssuedAccessToken;
import com.shop.auth.application.port.out.PasswordHasherPort;
import com.shop.auth.application.port.out.RefreshTokenStorePort;
import com.shop.auth.application.port.out.TokenBlacklistPort;
import com.shop.auth.application.port.out.UserRepositoryPort;
import com.shop.auth.application.service.AuthService;
import com.shop.auth.domain.exception.EmailAlreadyUsedException;
import com.shop.auth.domain.exception.InvalidCredentialsException;
import com.shop.auth.domain.exception.InvalidRefreshTokenException;
import com.shop.auth.domain.model.Email;
import com.shop.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test thuần: không cần Spring, DB hay Redis -> lợi ích chính của hexagonal. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepositoryPort users;
    @Mock PasswordHasherPort hasher;
    @Mock AccessTokenIssuerPort accessTokens;
    @Mock RefreshTokenStorePort refreshTokens;
    @Mock TokenBlacklistPort blacklist;

    AuthService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, hasher, accessTokens, refreshTokens, blacklist);
        user = User.register(new Email("a@b.com"), "hashed", "Alice");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(users.findByEmail(any(Email.class))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.register(new RegisterUserCommand("A@B.com", "password123", "Alice")))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verify(users, never()).save(any());
    }

    @Test
    void login_wrongPassword_issuesNoTokens() {
        when(users.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(hasher.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("a@b.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(refreshTokens, never()).create(any());
    }

    @Test
    void refresh_rotatesToken() {
        when(refreshTokens.consume("old")).thenReturn(Optional.of(user.id()));
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(accessTokens.issue(user)).thenReturn(new IssuedAccessToken("jwt", "jti", Instant.now().plusSeconds(900)));
        when(refreshTokens.create(user.id())).thenReturn("new");

        AuthTokens tokens = service.refresh("old");

        assertThat(tokens.refreshToken()).isEqualTo("new");
        assertThat(tokens.accessToken()).isEqualTo("jwt");
    }

    @Test
    void refresh_unknownToken_isRejected() {
        when(refreshTokens.consume("reused")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("reused")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logout_revokesRefreshAndBlacklistsAccessToken() {
        Instant exp = Instant.now().plusSeconds(600);

        service.logout(new LogoutCommand("rt", "jti-1", exp));

        verify(refreshTokens).revoke("rt");
        verify(blacklist).blacklist("jti-1", exp);
    }
}
