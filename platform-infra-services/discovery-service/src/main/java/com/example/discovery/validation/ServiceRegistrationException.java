package com.example.discovery.validation;

/**
 * Exception thrown when service registration validation fails
 */
public class ServiceRegistrationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public ServiceRegistrationException(String message) {
        super(message);
    }
    
    public ServiceRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
