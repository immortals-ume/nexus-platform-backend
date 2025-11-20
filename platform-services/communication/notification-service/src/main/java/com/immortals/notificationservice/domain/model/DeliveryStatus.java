package com.immortals.notificationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Real-time delivery status tracking
 * Updated via provider webhooks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatus {
    
    private Long id;
    private String notificationId;
    private String providerId;
    private String externalId; // Provider's message ID
    private Status status;
    private String statusDetails;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    private LocalDateTime bouncedAt;
    private String bounceReason;
    private LocalDateTime updatedAt;
    
    public enum Status {
        QUEUED,
        SENT,
        DELIVERED,
        OPENED,
        CLICKED,
        BOUNCED,
        FAILED,
        UNSUBSCRIBED
    }
    
    public boolean isDelivered() {
        return status == Status.DELIVERED || status == Status.OPENED || status == Status.CLICKED;
    }
    
    public boolean isFailed() {
        return status == Status.BOUNCED || status == Status.FAILED;
    }
}
