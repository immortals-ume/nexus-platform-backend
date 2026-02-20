package com.immortals.platform.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

public class BusinessException extends PlatformException {

    @Serial
    private static final long serialVersionUID = 15L;

    public BusinessException(String message) {
        super(message, ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode, ErrorCode.getHttpStatus(errorCode));
    }

    public BusinessException(String message, Throwable cause) {
        super(message, ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }

    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, ErrorCode.getHttpStatus(errorCode), cause);
    }
}
