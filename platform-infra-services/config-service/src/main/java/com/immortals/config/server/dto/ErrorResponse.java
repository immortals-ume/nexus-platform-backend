package com.immortals.config.server.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response DTO
 * Used across all error scenarios for consistent error handling
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Error type/category
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Request path that caused the error
     */
    private String path;
    
    /**
     * Timestamp when error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Correlation ID for tracing
     */
    private String correlationId;
    
    /**
     * Validation errors (if applicable)
     */
    private List<ValidationError> validationErrors;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
