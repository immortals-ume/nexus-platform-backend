package com.immortals.platform.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response structure for all API endpoints.
 * Provides consistent error handling across microservices.
 */
public record ErrorResponse(
    String error,
    String message,
    int status,
    String path,
    LocalDateTime timestamp,
    List<ValidationError> validationErrors
) {
    public static ErrorResponse of(String error, String message, int status, String path) {
        return new ErrorResponse(error, message, status, path, LocalDateTime.now(), null);
    }

    public static ErrorResponse withValidationErrors(
            String error, 
            String message, 
            int status, 
            String path,
            List<ValidationError> validationErrors) {
        return new ErrorResponse(error, message, status, path, LocalDateTime.now(), validationErrors);
    }

    public record ValidationError(
        String field,
        String message,
        Object rejectedValue
    ) {}
}
