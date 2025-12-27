package com.immortals.platform.common.exception;

import java.io.Serial;

/**
 * Exception thrown when template rendering fails
 */
public class TemplateRenderingException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    public TemplateRenderingException(String message) {
        super(message);
    }
    
    public TemplateRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
