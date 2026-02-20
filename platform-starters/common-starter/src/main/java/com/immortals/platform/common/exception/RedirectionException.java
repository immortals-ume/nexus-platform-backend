package com.immortals.platform.common.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception for redirection scenarios.
 * Used when a request needs to be redirected to another location.
 * 
 * Inheritance: RedirectionException -> PlatformException -> RuntimeException
 * Encapsulation: Encapsulates redirection codes, HTTP status, and redirect location
 * Polymorphism: Can be handled as PlatformException or RuntimeException
 */
public class RedirectionException extends PlatformException {

    @Serial
    private static final long serialVersionUID = 1L;
    
    private final String redirectLocation;

    public RedirectionException(String message, String redirectLocation) {
        super(message, ErrorCode.REDIRECT_FOUND, HttpStatus.FOUND);
        this.redirectLocation = redirectLocation;
    }

    public RedirectionException(String message, String code, String redirectLocation) {
        super(message, code, ErrorCode.getHttpStatus(code));
        this.redirectLocation = redirectLocation;
    }

    public RedirectionException(String message, String code, String redirectLocation, Throwable cause) {
        super(message, code, ErrorCode.getHttpStatus(code), cause);
        this.redirectLocation = redirectLocation;
    }

    public String getRedirectLocation() {
        return redirectLocation;
    }

    // Factory methods for common redirection scenarios
    public static RedirectionException movedPermanently(String message, String location) {
        return new RedirectionException(message, ErrorCode.REDIRECT_MOVED_PERMANENTLY, location);
    }

    public static RedirectionException found(String message, String location) {
        return new RedirectionException(message, ErrorCode.REDIRECT_FOUND, location);
    }

    public static RedirectionException temporaryRedirect(String message, String location) {
        return new RedirectionException(message, ErrorCode.REDIRECT_TEMPORARY, location);
    }

    public static RedirectionException permanentRedirect(String message, String location) {
        return new RedirectionException(message, ErrorCode.REDIRECT_PERMANENT, location);
    }
}