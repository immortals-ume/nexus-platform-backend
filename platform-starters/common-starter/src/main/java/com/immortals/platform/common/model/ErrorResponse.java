package com.immortals.platform.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response structure.
 * Provides consistent error information across all services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * HTTP status code
     */
    private int status;

    /**
     * HTTP status reason phrase (e.g., "Bad Request", "Not Found")
     */
    private String error;

    /**
     * Detailed error message
     */
    private String message;

    /**
     * The request path that caused the error
     */
    private String path;

    /**
     * Correlation ID for request tracing
     */
    private String correlationId;

    /**
     * Optional error code for programmatic error handling
     */
    private String errorCode;

    /**
     * List of validation errors (for validation failures)
     */
    private List<ValidationError> errors;

    /**
     * Creates an error response with basic information
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Creates an error response with correlation ID
     */
    public static ErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Creates an error response with validation errors
     */
    public static ErrorResponse withValidationErrors(int status, String error, String message, 
                                                     String path, String correlationId, 
                                                     List<ValidationError> errors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .errors(errors)
                .build();
    }
}
