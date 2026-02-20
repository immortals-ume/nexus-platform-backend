package com.immortals.platform.common.exception;

import org.springframework.http.HttpStatus;

public class SecurityException extends PlatformException {

    private static final long serialVersionUID = 1L;

    public SecurityException(String message) {
        super(message, ErrorCode.SECURITY_ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }

    public SecurityException(String message, String errorCode) {
        super(message, errorCode, ErrorCode.getHttpStatus(errorCode));
    }

    public SecurityException(String message, Throwable cause) {
        super(message, ErrorCode.SECURITY_ACCESS_DENIED, HttpStatus.FORBIDDEN, cause);
    }

    public SecurityException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, ErrorCode.getHttpStatus(errorCode), cause);
    }
}
