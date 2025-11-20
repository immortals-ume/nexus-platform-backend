package com.immortals.notificationservice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Generic webhook payload from notification providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    
    private String messageId;
    private String status;
    private String event;
    private Long timestamp;
    private Map<String, Object> metadata;
}
