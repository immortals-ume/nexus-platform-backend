package com.immortals.platform.common.exception;

import java.io.Serial;

/**
 * Exception for template rendering errors
 */
public class TemplateException extends NotificationException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String templateCode;
    
    public TemplateException(String message, String templateCode) {
        super(message);
        this.templateCode = templateCode;
    }
    
    public TemplateException(String message, String templateCode, Throwable cause) {
        super(message, cause);
        this.templateCode = templateCode;
    }
    
    public String getTemplateCode() {
        return templateCode;
    }
}
