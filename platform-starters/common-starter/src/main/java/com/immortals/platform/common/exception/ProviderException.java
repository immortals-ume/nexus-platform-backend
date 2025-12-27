package com.immortals.platform.common.exception;

import java.io.Serial;

/**
 * Exception for notification provider communication failures
 */
public class ProviderException extends NotificationException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String providerId;
    
    public ProviderException(String message, String providerId) {
        super(message);
        this.providerId = providerId;
    }
    
    public ProviderException(String message, String providerId, Throwable cause) {
        super(message, cause);
        this.providerId = providerId;
    }
    
    public String getProviderId() {
        return providerId;
    }
}
