package com.immortals.usermanagementservice.security.exception;

public class JwtNotFoundException extends Exception {
    public JwtNotFoundException(String message) {
        super(message);
    }
}