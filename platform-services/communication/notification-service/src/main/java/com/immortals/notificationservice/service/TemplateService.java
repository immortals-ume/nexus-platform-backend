package com.immortals.notificationservice.service;

import java.util.Map;

/**
 * Service for rendering notification templates
 */
public interface TemplateService {
    
    /**
     * Render template with variables
     */
    String renderTemplate(String templateCode, Map<String, Object> variables);
    
    /**
     * Render HTML template with variables
     */
    String renderHtmlTemplate(String templateCode, Map<String, Object> variables);
    
    /**
     * Check if template exists
     */
    boolean templateExists(String templateCode);
}
