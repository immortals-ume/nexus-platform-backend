package com.immortals.platform.common.exception;

/**
 * Exception thrown when cache operations fail.
 */
public class CacheException extends TechnicalException {

    private static final long serialVersionUID = 1L;

    public CacheException(String message) {
        super(message);
    }

    public CacheException(String message, String errorCode) {
        super(message, errorCode);
    }

    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
