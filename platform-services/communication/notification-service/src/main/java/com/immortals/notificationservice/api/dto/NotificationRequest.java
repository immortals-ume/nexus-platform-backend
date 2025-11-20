package com.immortals.notificationservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for notification request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification request payload")
public class NotificationRequest {
    
    @NotBlank(message = "Notification type is required")
    @Schema(description = "Type of notification", example = "EMAIL", allowableValues = {"EMAIL", "SMS", "WHATSAPP"})
    private String type;
    
    @NotBlank(message = "Recipient is required")
    @Schema(description = "Recipient email or phone number", example = "user@example.com")
    private String recipient;
    
    @Schema(description = "Email subject (required for EMAIL type)", example = "Welcome to our platform")
    private String subject;
    
    @NotBlank(message = "Message is required")
    @Schema(description = "Notification message content", example = "Thank you for signing up!")
    private String message;
    
    @Schema(description = "HTML content for email (optional)", example = "<h1>Welcome!</h1>")
    private String htmlContent;
    
    @Schema(description = "Correlation ID for tracing", example = "abc-123-def")
    private String correlationId;
}
