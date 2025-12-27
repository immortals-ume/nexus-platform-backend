package com.immortals.platform.domain.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for notification response
 * Requirements: 1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification response")
public class NotificationResponse {
    
    @Schema(description = "Notification ID", example = "123")
    private Long id;
    
    @Schema(description = "Event ID", example = "evt-123-abc")
    private String eventId;

    @Schema(description = "Notification type", example = "EMAIL", 
            allowableValues = {"EMAIL", "SMS", "WHATSAPP", "PUSH_NOTIFICATION"})
    private String type;
    
    @Schema(description = "Recipient", example = "user@example.com")
    private String recipient;
    
    @Schema(description = "Country code", example = "US")
    private String countryCode;
    
    @Schema(description = "Locale", example = "en_US")
    private String locale;
    
    @Schema(description = "Subject", example = "Welcome!")
    private String subject;
    
    @Schema(description = "Message content", example = "Thank you for signing up!")
    private String message;
    
    @Schema(description = "Template code", example = "welcome-email")
    private String templateCode;
    
    @Schema(description = "Notification status", example = "SENT", 
            allowableValues = {"PENDING", "SENT", "FAILED", "SCHEDULED", "CANCELLED"})
    private String status;
    
    @Schema(description = "Delivery status", example = "DELIVERED", 
            allowableValues = {"PENDING", "SENT", "DELIVERED", "FAILED", "READ"})
    private String deliveryStatus;
    
    @Schema(description = "Error message if failed", example = "Invalid recipient")
    private String errorMessage;
    
    @Schema(description = "Provider ID", example = "TWILIO")
    private String providerId;
    
    @Schema(description = "Provider message ID", example = "SM1234567890")
    private String providerMessageId;
    
    @Schema(description = "Correlation ID for tracing", example = "abc-123-def")
    private String correlationId;
    
    @Schema(description = "Scheduled delivery time", example = "2024-12-31T23:59:59")
    private LocalDateTime scheduledAt;
    
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Processing timestamp", example = "2024-01-15T10:30:05")
    private LocalDateTime processedAt;
    
    @Schema(description = "Delivery timestamp", example = "2024-01-15T10:30:10")
    private LocalDateTime deliveredAt;
    
    @Schema(description = "Read timestamp", example = "2024-01-15T10:35:00")
    private LocalDateTime readAt;
    
    @Schema(description = "Retry count", example = "0")
    private Integer retryCount;
    
    @Schema(description = "Additional metadata")
    private Map<String, String> metadata;
}
