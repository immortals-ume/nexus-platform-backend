package com.immortals.platform.common.exception;

/**
 * Exception for authentication failures.
 * Used when user authentication fails (invalid credentials, locked account, etc.)
 */
public class AuthenticationException extends SecurityException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
