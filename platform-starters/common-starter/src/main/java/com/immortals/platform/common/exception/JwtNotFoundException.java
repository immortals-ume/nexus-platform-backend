package com.immortals.platform.common.exception;

import java.io.Serial;

public class JwtNotFoundException extends SecurityException {
    @Serial
    private static final long serialVersionUID = 13L;
    public JwtNotFoundException(String message) {
        super(message);
    }
}