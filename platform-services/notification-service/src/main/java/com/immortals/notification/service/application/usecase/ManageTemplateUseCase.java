package com.immortals.notification.service.application.usecase;

import com.immortals.platform.domain.notifications.domain.NotificationTemplate;

import java.util.List;
import java.util.Map;

/**
 * Use case for managing notification templates
 * Handles CRUD operations and template rendering with caching
 */
public interface ManageTemplateUseCase {
    
    /**
     * Create a new notification template
     * 
     * @param template The template to create
     * @return The created template with generated ID
     */
    NotificationTemplate createTemplate(NotificationTemplate template);
    
    /**
     * Update an existing notification template
     * 
     * @param id The template ID
     * @param template The updated template data
     * @return The updated template
     */
    NotificationTemplate updateTemplate(Long id, NotificationTemplate template);
    
    /**
     * Delete a notification template (soft delete)
     * 
     * @param id The template ID to delete
     */
    void deleteTemplate(Long id);
    
    /**
     * Get a template by ID
     * 
     * @param id The template ID
     * @return The template
     */
    NotificationTemplate getTemplate(Long id);
    
    /**
     * Get a template by code and locale
     * Falls back to default locale if specific locale not found
     * 
     * @param templateCode The template code
     * @param locale The desired locale
     * @return The template
     */
    NotificationTemplate getTemplate(String templateCode, String locale);
    
    /**
     * Get all templates
     * 
     * @return List of all templates
     */
    List<NotificationTemplate> getAllTemplates();
    
    /**
     * Get all templates by channel
     * 
     * @param channel The notification channel
     * @return List of templates for the channel
     */
    List<NotificationTemplate> getTemplatesByChannel(String channel);
    
    /**
     * Render a template with provided variables
     * 
     * @param templateCode The template code
     * @param locale The locale
     * @param variables The variables to substitute
     * @return The rendered template content
     */
    String renderTemplate(String templateCode, String locale, Map<String, Object> variables);
}
