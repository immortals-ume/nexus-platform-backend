package com.immortals.notification.service.infra.mapper;

import com.immortals.platform.domain.notifications.domain.model.NotificationTemplate;
import com.immortals.platform.domain.notifications.entity.NotificationTemplateEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between NotificationTemplate domain model and entity
 */
@Component
public class NotificationTemplateMapper {
    
    public NotificationTemplate toModel(NotificationTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return NotificationTemplate.builder()
            .id(entity.getId() != null ? entity.getId().getMostSignificantBits() : null)
            .templateCode(entity.getTemplateCode())
            .templateName(entity.getTemplateName())
            .channel(entity.getChannel())
            .locale(entity.getLocale())
            .subject(entity.getSubject())
            .bodyTemplate(entity.getBodyTemplate())
            .htmlTemplate(entity.getHtmlTemplate())
            .engine(entity.getEngine())
            .active(entity.getActive())
            .defaultVariables(entity.getDefaultVariables())
            .build();
    }
    
    public NotificationTemplateEntity toEntity(NotificationTemplate model) {
        if (model == null) {
            return null;
        }
        
        return NotificationTemplateEntity.builder()
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
            .build();
    }
    
    public void updateEntity(NotificationTemplate model, NotificationTemplateEntity entity) {
        if (model == null || entity == null) {
            return;
        }
        
        if (model.getTemplateCode() != null) {
            entity.setTemplateCode(model.getTemplateCode());
        }
        if (model.getTemplateName() != null) {
            entity.setTemplateName(model.getTemplateName());
        }
        if (model.getChannel() != null) {
            entity.setChannel(model.getChannel());
        }
        if (model.getLocale() != null) {
            entity.setLocale(model.getLocale());
        }
        if (model.getSubject() != null) {
            entity.setSubject(model.getSubject());
        }
        if (model.getBodyTemplate() != null) {
            entity.setBodyTemplate(model.getBodyTemplate());
        }
        if (model.getHtmlTemplate() != null) {
            entity.setHtmlTemplate(model.getHtmlTemplate());
        }
        if (model.getEngine() != null) {
            entity.setEngine(model.getEngine());
        }
        if (model.getActive() != null) {
            entity.setActive(model.getActive());
        }
        if (model.getDefaultVariables() != null) {
            entity.setDefaultVariables(model.getDefaultVariables());
        }
    }
}
