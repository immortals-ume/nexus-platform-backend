package com.immortals.usermanagementservice.service.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Exception e) {
        super(message, e);
    }
}
