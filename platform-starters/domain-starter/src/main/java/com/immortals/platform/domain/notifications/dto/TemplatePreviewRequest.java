package com.immortals.platform.domain.notifications.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for previewing a template with sample variables
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePreviewRequest {
    
    @NotNull(message = "Variables are required for preview")
    private Map<String, Object> variables;
    
    @Builder.Default
    private String locale = "en_US";
}
