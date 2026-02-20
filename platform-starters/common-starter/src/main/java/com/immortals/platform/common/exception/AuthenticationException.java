package com.immortals.platform.common.exception;

import java.io.Serial;

public class AuthenticationException extends SecurityException {

    @Serial
    private static final long serialVersionUID = 13L;

    public AuthenticationException(String message) {
        super(message, ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, ErrorCode.AUTH_INVALID_CREDENTIALS, cause);
    }

    public AuthenticationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
