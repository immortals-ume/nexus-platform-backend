package com.immortals.notification.service.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for scheduled notification request
 * Extends basic notification request with scheduling capability
 * Requirements: 13.1, 13.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Scheduled notification request payload")
public class ScheduledNotificationRequest {
    
    @NotBlank(message = "Notification type is required")
    @Schema(description = "Type of notification", example = "EMAIL", 
            allowableValues = {"EMAIL", "SMS", "WHATSAPP", "PUSH_NOTIFICATION"})
    private String type;
    
    @NotBlank(message = "Recipient is required")
    @Schema(description = "Recipient email or phone number", example = "user@example.com")
    private String recipient;
    
    @Schema(description = "Email subject (required for EMAIL type)", example = "Welcome to our platform")
    private String subject;
    
    @Schema(description = "Notification message content", example = "Thank you for signing up!")
    private String message;
    
    @Schema(description = "HTML content for email (optional)", example = "<h1>Welcome!</h1>")
    private String htmlContent;
    
    @Schema(description = "Template code for rendering", example = "welcome-email")
    private String templateCode;
    
    @Schema(description = "Template variables for rendering")
    private Map<String, Object> templateVariables;
    
    @Schema(description = "Country code for provider routing", example = "US")
    private String countryCode;
    
    @Schema(description = "Locale for template selection", example = "en_US")
    private String locale;
    
    @Schema(description = "Correlation ID for tracing", example = "abc-123-def")
    private String correlationId;
    
    @Schema(description = "Additional metadata")
    private Map<String, String> metadata;
    
    @Schema(description = "Scheduled delivery time (optional, if not provided notification is sent immediately)", 
            example = "2024-12-31T23:59:59")
    private LocalDateTime scheduledAt;
}
