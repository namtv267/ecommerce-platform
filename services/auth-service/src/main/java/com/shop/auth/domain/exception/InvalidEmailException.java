package com.shop.auth.domain.exception;

public class InvalidEmailException extends DomainException {
    public InvalidEmailException(String value) {
        super("Invalid email: " + value);
    }
}
