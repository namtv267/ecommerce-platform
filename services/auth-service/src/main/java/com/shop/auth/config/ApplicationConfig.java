package com.shop.auth.config;

import com.shop.auth.application.port.out.AccessTokenIssuerPort;
import com.shop.auth.application.port.out.PasswordHasherPort;
import com.shop.auth.application.port.out.RefreshTokenStorePort;
import com.shop.auth.application.port.out.TokenBlacklistPort;
import com.shop.auth.application.port.out.UserRepositoryPort;
import com.shop.auth.application.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Nơi duy nhất "nối" application layer (thuần Java) với các adapter. */
@Configuration
class ApplicationConfig {

    @Bean
    AuthService authService(UserRepositoryPort users, PasswordHasherPort hasher, AccessTokenIssuerPort accessTokens,
                            RefreshTokenStorePort refreshTokens, TokenBlacklistPort blacklist) {
        return new AuthService(users, hasher, accessTokens, refreshTokens, blacklist);
    }
}
