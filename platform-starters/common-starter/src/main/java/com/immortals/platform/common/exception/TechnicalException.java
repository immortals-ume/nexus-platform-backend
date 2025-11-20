package com.immortals.platform.common.exception;

/**
 * Exception for technical/infrastructure failures.
 * Used when technical operations fail (database, cache, messaging, etc.).
 */
public class TechnicalException extends PlatformException {

    private static final long serialVersionUID = 1L;

    public TechnicalException(String message) {
        super(message);
    }

    public TechnicalException(String message, String errorCode) {
        super(message, errorCode);
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public TechnicalException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
