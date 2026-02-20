package com.immortals.notification.service.infra.template;

import com.immortals.platform.common.exception.TemplateRenderingException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * FreeMarker-based template renderer
 * Supports FreeMarker template syntax for variable substitution
 */
@Component
@Slf4j
public class FreeMarkerTemplateRenderer implements TemplateRenderer {
    
    private final Configuration freemarkerConfig;
    
    public FreeMarkerTemplateRenderer() {
        this.freemarkerConfig = createFreeMarkerConfiguration();
    }
    
    @Override
    public String render(String template, Map<String, Object> variables) {
        try {
            Template fmTemplate = new Template(
                "template", 
                new StringReader(template), 
                freemarkerConfig
            );
            
            StringWriter writer = new StringWriter();
            fmTemplate.process(variables != null ? variables : Map.of(), writer);
            
            return writer.toString();
            
        } catch (IOException e) {
            log.error("IO error rendering FreeMarker template: {}", e.getMessage());
            throw new TemplateRenderingException("FreeMarker template IO error", e);
        } catch (TemplateException e) {
            log.error("Failed to render FreeMarker template: {}", e.getMessage());
            throw new TemplateRenderingException("FreeMarker template rendering failed", e);
        } catch (Exception e) {
            log.error("Unexpected error rendering FreeMarker template: {}", e.getMessage());
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
        return "FREEMARKER";
    }
    
    /**
     * Create FreeMarker configuration for string-based templates
     */
    private Configuration createFreeMarkerConfiguration() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_32);
        config.setDefaultEncoding(Configuration.DEFAULT_ENCODING_KEY);
        config.setLogTemplateExceptions(Boolean.FALSE);
        config.setWrapUncheckedExceptions(Boolean.TRUE);
        
        return config;
    }
}
