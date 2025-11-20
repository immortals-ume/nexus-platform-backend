package com.immortals.notificationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain model for notification templates
 * Supports dynamic content with variable substitution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {
    
    private Long id;
    private String templateCode;
    private String templateName;
    private Notification.NotificationType type;
    private String subject;
    private String bodyTemplate;
    private String htmlTemplate;
    private TemplateEngine engine;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum TemplateEngine {
        THYMELEAF, FREEMARKER, PLAIN_TEXT
    }
    
    /**
     * Render template with variables
     */
    public String render(Map<String, Object> variables) {
        // Will be implemented by template service
        return bodyTemplate;
    }
}
