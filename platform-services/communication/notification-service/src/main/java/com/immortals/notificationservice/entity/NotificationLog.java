package com.immortals.notificationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to track notification processing for idempotency
 */
@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_correlation_id", columnList = "correlation_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;
    
    @Column(name = "notification_type", nullable = false)
    private String notificationType;
    
    @Column(name = "recipient", nullable = false)
    private String recipient;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "status", nullable = false)
    private String status; // PENDING, SENT, FAILED
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "correlation_id")
    private String correlationId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}
