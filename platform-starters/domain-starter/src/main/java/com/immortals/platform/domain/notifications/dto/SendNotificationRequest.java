package com.immortals.platform.domain.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for sending notification request with all options
 * This is the comprehensive request DTO that includes scheduling and all advanced features
 * Requirements: 1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Send notification request payload with all options")
public class SendNotificationRequest {
    
    @NotBlank(message = "Notification type is required")
    @Pattern(regexp = "EMAIL|SMS|WHATSAPP|PUSH_NOTIFICATION", 
             message = "Type must be one of: EMAIL, SMS, WHATSAPP, PUSH_NOTIFICATION")
    @Schema(description = "Type of notification", example = "EMAIL", 
            allowableValues = {"EMAIL", "SMS", "WHATSAPP", "PUSH_NOTIFICATION"})
    private String type;
    
    @NotBlank(message = "Recipient is required")
    @Size(max = 255, message = "Recipient must not exceed 255 characters")
    @Schema(description = "Recipient email or phone number", example = "user@example.com")
    private String recipient;
    
    @Size(max = 10, message = "Country code must not exceed 10 characters")
    @Schema(description = "Country code for provider routing", example = "US")
    private String countryCode;
    
    @Size(max = 10, message = "Locale must not exceed 10 characters")
    @Schema(description = "Locale for template selection", example = "en_US")
    private String locale;
    
    @Size(max = 500, message = "Subject must not exceed 500 characters")
    @Schema(description = "Email subject (required for EMAIL type)", example = "Welcome to our platform")
    private String subject;
    
    @Size(max = 10000, message = "Message must not exceed 10000 characters")
    @Schema(description = "Notification message content (required if templateCode is not provided)", 
            example = "Thank you for signing up!")
    private String message;
    
    @Schema(description = "HTML content for email (optional)", example = "<h1>Welcome!</h1>")
    private String htmlContent;
    
    @Size(max = 100, message = "Template code must not exceed 100 characters")
    @Schema(description = "Template code for rendering", example = "welcome-email")
    private String templateCode;
    
    @Schema(description = "Template variables for rendering", 
            example = "{\"userName\": \"John Doe\", \"activationLink\": \"https://example.com/activate\"}")
    private Map<String, Object> templateVariables;
    
    @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT", flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "Priority must be one of: LOW, NORMAL, HIGH, URGENT")
    @Schema(description = "Notification priority", example = "NORMAL", 
            allowableValues = {"LOW", "NORMAL", "HIGH", "URGENT"})
    private String priority;
    
    @Size(max = 255, message = "Correlation ID must not exceed 255 characters")
    @Schema(description = "Correlation ID for tracing", example = "abc-123-def")
    private String correlationId;
    
    @Schema(description = "Additional metadata", 
            example = "{\"source\": \"user-registration\", \"campaign\": \"welcome-series\"}")
    private Map<String, String> metadata;
    
    @Schema(description = "Scheduled delivery time (optional, if not provided notification is sent immediately)", 
            example = "2024-12-31T23:59:59")
    private LocalDateTime scheduledAt;
}
