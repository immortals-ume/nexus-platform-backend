package com.immortals.usermanagementservice.service.exception;


public class CacheException extends RuntimeException {
    public CacheException(String message) {
        super(message);
    }

    public CacheException(String message, Exception e) {
        super(message, e);
    }
}
