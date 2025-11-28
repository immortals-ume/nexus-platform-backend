package com.immortals.cache.features.circuitbreaker;

/**
 * Exception thrown when a cache operation fails due to circuit breaker being open.
 */
public class CircuitBreakerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CircuitBreakerException(String message) {
        super(message);
    }

    public CircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }
}
