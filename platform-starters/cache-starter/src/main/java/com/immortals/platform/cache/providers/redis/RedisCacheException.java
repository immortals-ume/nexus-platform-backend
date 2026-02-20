package com.immortals.platform.cache.providers.redis;

import java.io.Serial;

/**
 * Exception thrown when Redis cache operations fail.
 */
public class RedisCacheException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RedisCacheException(String message) {
        super(message);
    }

    public RedisCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
