package com.immortals.platform.common.exception;

import java.io.Serial;

/**
 * Base exception for notification-related errors
 */
public class NotificationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    public NotificationException(String message) {
        super(message);
    }
    
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
