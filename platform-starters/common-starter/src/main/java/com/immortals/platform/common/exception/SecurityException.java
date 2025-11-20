package com.immortals.platform.common.exception;

/**
 * Exception for security-related failures.
 * Used for authentication and authorization failures.
 */
public class SecurityException extends PlatformException {

    private static final long serialVersionUID = 1L;

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, String errorCode) {
        super(message, errorCode);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecurityException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
