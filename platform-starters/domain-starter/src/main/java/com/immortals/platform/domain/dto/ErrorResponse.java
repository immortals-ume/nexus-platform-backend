package com.immortals.platform.domain.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response structure for all API endpoints.
 * Provides consistent error handling across microservices.
 */
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String correlationId,
    String errorCode,
    List<ValidationError> errors
) {
    /**
     * Creates an error response with basic information
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null, null, null);
    }

    /**
     * Creates an error response with correlation ID
     */
    public static ErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, error, message, path, correlationId, null, null);
    }

    /**
     * Creates an error response with correlation ID and error code
     */
    public static ErrorResponse of(int status, String error, String message, String path, String correlationId, String errorCode) {
        return new ErrorResponse(Instant.now(), status, error, message, path, correlationId, errorCode, null);
    }

    /**
     * Creates an error response with validation errors
     */
    public static ErrorResponse withValidationErrors(
            int status,
            String error,
            String message,
            String path,
            String correlationId,
            List<ValidationError> errors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, correlationId, null, errors);
    }

    /**
     * Validation error record for field-level validation failures
     */
    public record ValidationError(
        String field,
        Object rejectedValue,
        String message,
        String code
    ) {
        /**
         * Creates a validation error with field and message
         */
        public static ValidationError of(String field, String message) {
            return new ValidationError(field, null, message, null);
        }

        /**
         * Creates a validation error with field, rejected value, and message
         */
        public static ValidationError of(String field, Object rejectedValue, String message) {
            return new ValidationError(field, rejectedValue, message, null);
        }

        /**
         * Creates a validation error with all fields
         */
        public static ValidationError of(String field, Object rejectedValue, String message, String code) {
            return new ValidationError(field, rejectedValue, message, code);
        }
    }
}
