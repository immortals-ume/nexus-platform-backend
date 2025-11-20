package com.immortals.platform.common.exception;

/**
 * Exception for business logic violations.
 * Used when business rules are not satisfied.
 */
public class BusinessException extends PlatformException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
