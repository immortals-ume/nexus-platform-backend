package com.immortals.platform.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic wrapper for API responses.
 * Provides a consistent response structure across all services.
 *
 * @param <T> The type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * The actual response data
     */
    private T data;

    /**
     * Optional message providing additional context
     */
    private String message;

    /**
     * Timestamp when the response was generated
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Correlation ID for request tracing
     */
    private String correlationId;

    /**
     * Creates a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a successful response with data and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a successful response with data, message, and correlation ID
     */
    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates a response with only a message
     */
    public static <T> ApiResponse<T> message(String message) {
        return ApiResponse.<T>builder()
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
