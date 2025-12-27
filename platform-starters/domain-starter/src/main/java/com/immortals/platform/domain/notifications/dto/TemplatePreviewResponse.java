package com.immortals.platform.domain.notifications.dto;

import lombok.*;

/**
 * Response DTO for template preview
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePreviewResponse {
    
    private String renderedContent;
    private String templateCode;
    private String locale;
}
