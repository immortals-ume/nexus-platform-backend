package com.immortals.platform.domain.dto;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper using Java 21 Record.
 * Provides consistent response structure across all services.
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    LocalDateTime timestamp
) {
    /**
     * Create successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    /**
     * Create successful response with data and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now());
    }

    /**
     * Create error response with message
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now());
    }

    /**
     * Create error response with data and message
     */
    public static <T> ApiResponse<T> error(T data, String message) {
        return new ApiResponse<>(false, data, message, LocalDateTime.now());
    }
}