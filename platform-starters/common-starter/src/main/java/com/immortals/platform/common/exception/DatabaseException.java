package com.immortals.platform.common.exception;

/**
 * Exception thrown when database operations fail.
 */
public class DatabaseException extends TechnicalException {

    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
