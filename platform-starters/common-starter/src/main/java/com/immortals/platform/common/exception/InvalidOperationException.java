package com.immortals.platform.common.exception;

public class InvalidOperationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidOperationException(String message) {
        super(message);
    }

    public InvalidOperationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidOperationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
