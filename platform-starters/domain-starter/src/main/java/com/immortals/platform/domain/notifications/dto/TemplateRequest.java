package com.immortals.platform.domain.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for creating or updating notification templates
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateRequest {
    
    @NotBlank(message = "Template code is required")
    private String templateCode;
    
    @NotBlank(message = "Template name is required")
    private String templateName;
    
    @NotBlank(message = "Channel is required")
    private String channel;
    
    @Builder.Default
    private String locale = "en_US";
    
    private String subject;
    
    @NotBlank(message = "Body template is required")
    private String bodyTemplate;
    
    private String htmlTemplate;
    
    @NotBlank(message = "Template engine is required")
    @Builder.Default
    private String engine = "PLAIN_TEXT";
    
    @Builder.Default
    private Boolean active = true;
    
    private Map<String, String> defaultVariables;
}
