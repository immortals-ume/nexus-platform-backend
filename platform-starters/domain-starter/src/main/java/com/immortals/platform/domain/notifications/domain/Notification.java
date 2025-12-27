package com.immortals.platform.domain.notifications.domain;

import com.immortals.platform.domain.BaseEntity;
import lombok.*;

import java.time.Instant;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    private Long id;
    private String eventId;
    private NotificationType type;
    private NotificationPriority priority;
    private String recipient;
    private String countryCode;
    private String locale;
    private String subject;
    private String message;
    private String htmlContent;
    private String templateCode;
    private Map<String, Object> templateVariables;
    private NotificationStatus status;
    private DeliveryStatus deliveryStatus;
    private String errorMessage;
    private String correlationId;
    private String providerId;
    private String providerMessageId;
    private Instant createdAt;
    private Instant processedAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant scheduledAt;
    private Integer retryCount;
    private Integer maxRetries;
    private Map<String, String> metadata;
    
    /**
     * Notification types supported by the system
     */
    public enum NotificationType {
        EMAIL, SMS, WHATSAPP, PUSH_NOTIFICATION
    }

    /**
     * Notification processing status
     */
    public enum NotificationStatus {
        PENDING, SENT, FAILED, SCHEDULED, CANCELLED
    }

    /**
     * Delivery status from provider
     */
    public enum DeliveryStatus {
        PENDING, SENT, DELIVERED, FAILED, READ
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
        this.processedAt = Instant.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = Instant.now();
    }
    
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
    
    public boolean isDelivered() {
        return deliveryStatus == DeliveryStatus.DELIVERED;
    }
    
    public boolean isRead() {
        return deliveryStatus == DeliveryStatus.READ;
    }
    
    public void markAsDelivered(Instant deliveredAt) {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }
    
    public void markAsRead(Instant readAt) {
        this.deliveryStatus = DeliveryStatus.READ;
        this.readAt = readAt;
    }
    
    public void updateDeliveryStatus(DeliveryStatus status) {
        this.deliveryStatus = status;
        if (status == DeliveryStatus.DELIVERED && this.deliveredAt == null) {
            this.deliveredAt = Instant.now();
        } else if (status == DeliveryStatus.READ && this.readAt == null) {
            this.readAt = Instant.now();
        }
    }
}
