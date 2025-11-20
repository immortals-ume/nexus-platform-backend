package com.immortals.platform.common.exception;

/**
 * Abstract base exception for all platform exceptions.
 * All custom exceptions in the platform should extend this class.
 */
public abstract class PlatformException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String errorCode;

    protected PlatformException(String message) {
        super(message);
        this.errorCode = null;
    }

    protected PlatformException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected PlatformException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    protected PlatformException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
