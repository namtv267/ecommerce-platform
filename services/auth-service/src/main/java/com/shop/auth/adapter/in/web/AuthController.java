package com.shop.auth.adapter.in.web;

import com.shop.auth.adapter.in.web.dto.LoginRequest;
import com.shop.auth.adapter.in.web.dto.LogoutRequest;
import com.shop.auth.adapter.in.web.dto.RefreshRequest;
import com.shop.auth.adapter.in.web.dto.RegisterRequest;
import com.shop.auth.adapter.in.web.dto.TokenResponse;
import com.shop.auth.application.port.in.LoginUseCase;
import com.shop.auth.application.port.in.LoginUseCase.LoginCommand;
import com.shop.auth.application.port.in.LogoutUseCase;
import com.shop.auth.application.port.in.LogoutUseCase.LogoutCommand;
import com.shop.auth.application.port.in.RefreshTokenUseCase;
import com.shop.auth.application.port.in.RegisterUserUseCase;
import com.shop.auth.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.shop.auth.application.port.in.RegisterUserUseCase.RegisteredUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginUseCase login;
    private final RefreshTokenUseCase refreshToken;
    private final LogoutUseCase logout;

    AuthController(RegisterUserUseCase registerUser, LoginUseCase login,
                   RefreshTokenUseCase refreshToken, LogoutUseCase logout) {
        this.registerUser = registerUser;
        this.login = login;
        this.refreshToken = refreshToken;
        this.logout = logout;
    }

    @PostMapping("/register")
    ResponseEntity<RegisteredUser> register(@Valid @RequestBody RegisterRequest req) {
        RegisteredUser user = registerUser.register(new RegisterUserCommand(req.email(), req.password(), req.fullName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return TokenResponse.from(login.login(new LoginCommand(req.email(), req.password())));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return TokenResponse.from(refreshToken.refresh(req.refreshToken()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LogoutRequest req) {
        logout.logout(new LogoutCommand(req.refreshToken(), jwt.getId(), jwt.getExpiresAt()));
        return ResponseEntity.noContent().build();
    }

    /** Endpoint tiện để kiểm tra token: trả về thông tin lấy từ chính JWT. */
    @GetMapping("/me")
    Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "userId", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "roles", jwt.getClaimAsStringList("roles"));
    }
}
