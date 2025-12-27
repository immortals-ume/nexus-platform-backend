package com.immortals.notification.service.infra.mapper;

import com.immortals.platform.domain.notifications.dto.TemplateRequest;
import com.immortals.platform.domain.notifications.dto.TemplateResponse;
import com.immortals.platform.domain.notifications.domain.model.NotificationTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between template DTOs and domain models
 */
@Component
public class TemplateDtoMapper {
    
    public NotificationTemplate toModel(TemplateRequest request) {
        if (request == null) {
            return null;
        }
        
        return NotificationTemplate.builder()
            .templateCode(request.getTemplateCode())
            .templateName(request.getTemplateName())
            .channel(request.getChannel())
            .locale(request.getLocale())
            .subject(request.getSubject())
            .bodyTemplate(request.getBodyTemplate())
            .htmlTemplate(request.getHtmlTemplate())
            .engine(request.getEngine())
            .active(request.getActive())
            .defaultVariables(request.getDefaultVariables())
            .build();
    }
    
    public TemplateResponse toResponse(NotificationTemplate model) {
        if (model == null) {
            return null;
        }
        
        return TemplateResponse.builder()
            .id(model.getId())
            .templateCode(model.getTemplateCode())
            .templateName(model.getTemplateName())
            .channel(model.getChannel())
            .locale(model.getLocale())
            .subject(model.getSubject())
            .bodyTemplate(model.getBodyTemplate())
            .htmlTemplate(model.getHtmlTemplate())
            .engine(model.getEngine())
            .active(model.getActive())
            .defaultVariables(model.getDefaultVariables())
            .createdAt(model.getCreatedAt())
            .updatedAt(model.getUpdatedAt())
            .build();
    }
    
    public List<TemplateResponse> toResponseList(List<NotificationTemplate> models) {
        if (models == null) {
            return null;
        }
        
        return models.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
}
