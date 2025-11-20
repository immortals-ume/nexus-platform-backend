package com.immortals.platform.common.exception;

/**
 * Exception for user-related operations.
 * Used when user operations fail (user not found, invalid user state, etc.)
 */
public class UserException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public UserException(String message) {
        super(message);
    }

    public UserException(String message, String errorCode) {
        super(message, errorCode);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
