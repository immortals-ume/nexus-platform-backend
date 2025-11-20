package com.immortals.platform.common.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId), "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
