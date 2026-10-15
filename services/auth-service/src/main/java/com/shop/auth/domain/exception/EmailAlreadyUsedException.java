package com.shop.auth.domain.exception;

public class EmailAlreadyUsedException extends DomainException {
    public EmailAlreadyUsedException(String email) {
        super("Email already registered: " + email);
    }
}
