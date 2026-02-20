package com.immortals.notification.service.infra.template;

import com.immortals.platform.common.exception.TemplateRenderingException;

import java.util.Map;

/**
 * Interface for rendering notification templates with variable substitution
 * Supports multiple template engines (Thymeleaf, FreeMarker, Plain Text)
 */
public interface TemplateRenderer {
    
    /**
     * Render a template with the provided variables
     * 
     * @param template The template content to render
     * @param variables The variables to substitute in the template
     * @return The rendered template content
     * @throws TemplateRenderingException if rendering fails
     */
    String render(String template, Map<String, Object> variables);
    
    /**
     * Validate that a template is syntactically correct
     * 
     * @param template The template content to validate
     * @return true if the template is valid, false otherwise
     */
    boolean validate(String template);
    
    /**
     * Get the template engine type this renderer supports
     * 
     * @return The template engine type (THYMELEAF, FREEMARKER, PLAIN_TEXT)
     */
    String getEngineType();
}
