package com.immortals.authapp.service.exception;

public class UserException extends RuntimeException {
    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Exception e) {
        super(message, e);
    }
}