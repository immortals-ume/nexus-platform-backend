package com.immortals.usermanagementservice.service.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entity, Object id) {
        super(String.format("%s with ID %s not found", entity, id));
    }
}
