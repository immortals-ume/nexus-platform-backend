package com.immortals.platform.common.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceType, String identifier) {
        super(String.format("%s with identifier '%s' already exists", resourceType, identifier), "DUPLICATE_RESOURCE");
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
