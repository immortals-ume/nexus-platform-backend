package com.immortals.platform.common.exception;

/**
 * Exception thrown when a business rule is violated.
 * Used for domain-specific business logic violations that are not simple validation errors.
 * Returns HTTP 422 (Unprocessable Entity) to indicate the request was well-formed but semantically incorrect.
 */
public class BusinessRuleViolationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleViolationException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleViolationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, "BUSINESS_RULE_VIOLATION", cause);
    }

    public BusinessRuleViolationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
