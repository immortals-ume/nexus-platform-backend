package com.immortals.platform.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a field-level validation error.
 * Used in error responses to provide detailed validation failure information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError {

    /**
     * The field that failed validation
     */
    private String field;

    /**
     * The rejected value (optional, for debugging)
     */
    private Object rejectedValue;

    /**
     * The validation error message
     */
    private String message;

    /**
     * Optional error code for programmatic handling
     */
    private String code;

    /**
     * Creates a validation error with field and message
     */
    public static ValidationError of(String field, String message) {
        return ValidationError.builder()
                .field(field)
                .message(message)
                .build();
    }

    /**
     * Creates a validation error with field, rejected value, and message
     */
    public static ValidationError of(String field, Object rejectedValue, String message) {
        return ValidationError.builder()
                .field(field)
                .rejectedValue(rejectedValue)
                .message(message)
                .build();
    }

    /**
     * Creates a validation error with all fields
     */
    public static ValidationError of(String field, Object rejectedValue, String message, String code) {
        return ValidationError.builder()
                .field(field)
                .rejectedValue(rejectedValue)
                .message(message)
                .code(code)
                .build();
    }
}
