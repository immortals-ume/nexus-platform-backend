package com.immortals.authapp.security.exception;

public class JwtNotFoundException extends Exception {
    public JwtNotFoundException(String message) {
        super(message);
    }
}