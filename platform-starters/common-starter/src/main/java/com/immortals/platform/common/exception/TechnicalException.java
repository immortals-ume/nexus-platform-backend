package com.immortals.platform.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

public class TechnicalException extends PlatformException {

    @Serial
    private static final long serialVersionUID = 12L;

    public TechnicalException(String message) {
        super(message, ErrorCode.TECH_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public TechnicalException(String message, String errorCode) {
        super(message, errorCode, ErrorCode.getHttpStatus(errorCode));
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, ErrorCode.TECH_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    public TechnicalException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, ErrorCode.getHttpStatus(errorCode), cause);
    }
}
