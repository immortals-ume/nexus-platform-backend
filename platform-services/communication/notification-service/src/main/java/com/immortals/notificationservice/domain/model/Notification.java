package com.immortals.notificationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain model for Notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    private Long id;
    private String eventId;
    private NotificationType type;
    private NotificationPriority priority;
    private String recipient;
    private String subject;
    private String message;
    private String htmlContent;
    private String templateCode;
    private Map<String, Object> templateVariables;
    private NotificationStatus status;
    private String errorMessage;
    private String correlationId;
    private String providerId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime scheduledAt;
    private Integer retryCount;
    private Integer maxRetries;
    
    /**
     * Notification types supported by the system
     */
    public enum NotificationType {
        EMAIL, SMS, WHATSAPP
    }
    
    /**
     * Notification processing status
     */
    public enum NotificationStatus {
        PENDING, SENT, FAILED
    }
    
    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }
    
    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }
    
    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }
    
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }
    
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}
