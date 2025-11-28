package com.immortals.platform.common.exception;

import java.io.Serial;

/**
 * Exception thrown when a business rule is violated.
 * Used for domain-specific business logic violations that are not simple validation errors.
 * Returns HTTP 422 (Unprocessable Entity) to indicate the request was well-formed but semantically incorrect.
 */
public class BusinessRuleViolationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 17L;

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
