package com.immortals.otpservice.security.exception;

public class JwtNotFoundException extends Exception {
    public JwtNotFoundException(String message) {
        super(message);
    }
}