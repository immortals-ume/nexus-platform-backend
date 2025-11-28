package com.immortals.platform.domain.dto;

import java.time.Instant;

/**
 * Generic API response wrapper using Java 21 Record.
 * Provides consistent response structure across all services.
 *
 * @param <T> The type of data being returned
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    Instant timestamp,
    String correlationId
) {
    /**
     * Create successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), null);
    }

    /**
     * Create successful response with data and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now(), null);
    }

    /**
     * Create successful response with data, message, and correlation ID
     */
    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return new ApiResponse<>(true, data, message, Instant.now(), correlationId);
    }

    /**
     * Create response with only a message
     */
    public static <T> ApiResponse<T> message(String message) {
        return new ApiResponse<>(true, null, message, Instant.now(), null);
    }

    /**
     * Create error response with message
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), null);
    }

    /**
     * Create error response with data and message
     */
    public static <T> ApiResponse<T> error(T data, String message) {
        return new ApiResponse<>(false, data, message, Instant.now(), null);
    }

    /**
     * Create error response with message and correlation ID
     */
    public static <T> ApiResponse<T> error(String message, String correlationId) {
        return new ApiResponse<>(false, null, message, Instant.now(), correlationId);
    }
}