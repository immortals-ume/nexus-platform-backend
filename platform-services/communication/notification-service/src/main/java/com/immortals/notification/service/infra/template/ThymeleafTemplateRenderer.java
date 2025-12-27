package com.immortals.notification.service.infra.template;

import com.immortals.platform.common.exception.TemplateRenderingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

/**
 * Thymeleaf-based template renderer
 * Supports HTML and text templates with Thymeleaf syntax
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ThymeleafTemplateRenderer implements TemplateRenderer {
    
    private final TemplateEngine stringTemplateEngine;
    
    public ThymeleafTemplateRenderer() {
        this.stringTemplateEngine = createStringTemplateEngine();
    }
    
    @Override
    public String render(String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            
            return stringTemplateEngine.process(template, context);
            
        } catch (TemplateProcessingException e) {
            log.error("Failed to render Thymeleaf template: {}", e.getMessage());
            throw new TemplateRenderingException("Thymeleaf template rendering failed", e);
        } catch (Exception e) {
            log.error("Unexpected error rendering Thymeleaf template: {}", e.getMessage());
            throw new TemplateRenderingException("Unexpected error during template rendering", e);
        }
    }
    
    @Override
    public boolean validate(String template) {
        try {
            render(template, Map.of());
            return true;
        } catch (Exception e) {
            log.debug("Template validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getEngineType() {
        return "THYMELEAF";
    }
    
    /**
     * Create a TemplateEngine configured for string-based templates
     */
    private TemplateEngine createStringTemplateEngine() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);
        
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(templateResolver);
        
        return engine;
    }
}
