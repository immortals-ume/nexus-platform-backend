package com.immortals.platform.common.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when validation fails.
 * Used for input validation errors and constraint violations.
 */
public class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;
    private transient List<ValidationError> validationErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }

    public ValidationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(String message, List<ValidationError> validationErrors) {
        super(message, "VALIDATION_ERROR");
        this.validationErrors = validationErrors;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors != null ? validationErrors : new ArrayList<>();
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Inner class to represent individual validation errors
     */
    public static class ValidationError implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private String field;
        private String message;
        private transient Object rejectedValue;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
    }
}
