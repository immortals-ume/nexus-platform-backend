package com.immortals.platform.domain.notifications.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain model for notification templates
 * Represents a reusable notification content structure with variable substitution
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {
    
    private Long id;
    private String templateCode;
    private String templateName;
    private String channel;
    private String locale;
    private String subject;
    private String bodyTemplate;
    private String htmlTemplate;
    private String engine;
    private Boolean active;
    private Map<String, String> defaultVariables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Check if template is active and usable
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active);
    }
    
    /**
     * Validate template has required fields
     */
    public boolean isValid() {
        return templateCode != null && !templateCode.isBlank()
            && templateName != null && !templateName.isBlank()
            && channel != null && !channel.isBlank()
            && bodyTemplate != null && !bodyTemplate.isBlank()
            && engine != null && !engine.isBlank();
    }
}
