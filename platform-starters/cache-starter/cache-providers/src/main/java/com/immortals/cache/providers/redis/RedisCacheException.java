package com.immortals.cache.providers.redis;

/**
 * Exception thrown when Redis cache operations fail.
 */
public class RedisCacheException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RedisCacheException(String message) {
        super(message);
    }

    public RedisCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
