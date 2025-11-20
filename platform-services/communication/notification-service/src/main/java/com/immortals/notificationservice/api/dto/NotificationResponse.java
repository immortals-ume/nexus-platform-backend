package com.immortals.notificationservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for notification response
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
    
    @Schema(description = "Notification status", example = "SENT", allowableValues = {"PENDING", "SENT", "FAILED"})
    private String status;
}
