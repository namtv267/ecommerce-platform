package com.shop.auth.domain.exception;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, expired or already used");
    }
}
