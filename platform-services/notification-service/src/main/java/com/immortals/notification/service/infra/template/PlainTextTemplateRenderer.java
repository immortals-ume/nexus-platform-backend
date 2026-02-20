package com.immortals.notification.service.infra.template;

import com.immortals.platform.common.exception.TemplateRenderingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plain text template renderer with simple variable substitution
 * Supports ${variableName} syntax for variable replacement
 */
@Component
@Slf4j
public class PlainTextTemplateRenderer implements TemplateRenderer {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    @Override
    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            throw new TemplateRenderingException("Template cannot be null");
        }
        
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        
        try {
            StringBuilder result = new StringBuilder();
            Matcher matcher = VARIABLE_PATTERN.matcher(template);
            
            while (matcher.find()) {
                String variableName = matcher.group(1);
                Object value = variables.get(variableName);
                
                String replacement = value != null ? value.toString() : "";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            
            matcher.appendTail(result);
            return result.toString();
            
        } catch (Exception e) {
            log.error("Failed to render plain text template: {}", e.getMessage());
            throw new TemplateRenderingException("Plain text template rendering failed", e);
        }
    }
    
    @Override
    public boolean validate(String template) {
        if (template == null) {
            return false;
        }
        
        try {
            int openBraces = 0;
            for (int i = 0; i < template.length(); i++) {
                char c = template.charAt(i);
                if (c == '{' && i > 0 && template.charAt(i - 1) == '$') {
                    openBraces++;
                } else if (c == '}' && openBraces > 0) {
                    openBraces--;
                }
            }
            
            return openBraces == 0;
        } catch (Exception e) {
            log.debug("Template validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getEngineType() {
        return "PLAIN_TEXT";
    }
}
