package com.immortals.platform.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

@Getter
public abstract class PlatformException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String errorCode;
    private final HttpStatus httpStatus;

    protected PlatformException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected PlatformException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public int getHttpStatusValue() {
        return httpStatus.value();
    }
}
