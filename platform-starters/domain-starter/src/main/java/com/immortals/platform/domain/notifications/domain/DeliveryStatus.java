package com.immortals.platform.domain.notifications.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatus {
    
    private Long id;
    private String notificationId;
    private String providerId;
    private String externalId;
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
