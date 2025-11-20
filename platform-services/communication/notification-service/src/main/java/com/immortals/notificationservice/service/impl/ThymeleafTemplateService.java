package com.immortals.notificationservice.service.impl;

import com.immortals.notificationservice.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Thymeleaf-based template rendering service
 * Supports dynamic HTML email templates with variable substitution
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ThymeleafTemplateService implements TemplateService {
    
    private final TemplateEngine templateEngine;
    
    @Override
    public String renderTemplate(String templateCode, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            return templateEngine.process(templateCode, context);
            
        } catch (Exception e) {
            log.error("Failed to render template: {}, error: {}", templateCode, e.getMessage());
            throw new RuntimeException("Template rendering failed", e);
        }
    }
    
    @Override
    public String renderHtmlTemplate(String templateCode, Map<String, Object> variables) {
        return renderTemplate("email/" + templateCode, variables);
    }
    
    @Override
    public boolean templateExists(String templateCode) {
        try {
            templateEngine.process(templateCode, new Context());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
