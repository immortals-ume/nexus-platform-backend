package com.immortals.platform.domain.notifications.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for notification templates
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {
    
    private Long id;
    private String templateCode;
    private String templateName;
    private String channel;
    private String locale;
    private String subject;
    private String bodyTemplate;
    private String htmlTemplate;
    private String engine;
    private Boolean active;
    private Map<String, String> defaultVariables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
