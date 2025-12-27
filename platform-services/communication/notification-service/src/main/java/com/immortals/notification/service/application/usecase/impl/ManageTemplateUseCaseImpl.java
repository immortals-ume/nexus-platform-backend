package com.immortals.notification.service.application.usecase.impl;

import com.immortals.notification.service.application.usecase.ManageTemplateUseCase;
import com.immortals.platform.domain.notifications.domain.model.NotificationTemplate;
import com.immortals.platform.domain.notifications.entity.NotificationTemplateEntity;
import com.immortals.notification.service.infra.mapper.NotificationTemplateMapper;
import com.immortals.notification.service.infra.template.TemplateRenderer;
import com.immortals.platform.common.exception.TemplateRenderingException;
import com.immortals.notification.service.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ManageTemplateUseCase
 * Provides template management with caching support
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ManageTemplateUseCaseImpl implements ManageTemplateUseCase {
    
    private static final String DEFAULT_LOCALE = "en_US";
    private static final String TEMPLATE_CACHE = "notification:template";
    
    private final NotificationTemplateRepository templateRepository;
    private final NotificationTemplateMapper templateMapper;
    private final List<TemplateRenderer> templateRenderers;
    
    @Override
    @Transactional
    @CacheEvict(value = TEMPLATE_CACHE, allEntries = true)
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        log.info("Creating template: code={}, locale={}", template.getTemplateCode(), template.getLocale());
        
        // Validate template
        if (!template.isValid()) {
            throw new IllegalArgumentException("Template validation failed: missing required fields");
        }
        
        // Check if template already exists
        if (templateRepository.existsByTemplateCodeAndLocale(
                template.getTemplateCode(), 
                template.getLocale() != null ? template.getLocale() : DEFAULT_LOCALE)) {
            throw new IllegalArgumentException(
                String.format("Template already exists: code=%s, locale=%s", 
                    template.getTemplateCode(), template.getLocale())
            );
        }
        
        // Set default locale if not provided
        if (template.getLocale() == null || template.getLocale().isBlank()) {
            template.setLocale(DEFAULT_LOCALE);
        }
        
        // Set default active status
        if (template.getActive() == null) {
            template.setActive(true);
        }
        
        // Validate template syntax
        TemplateRenderer renderer = getRendererForEngine(template.getEngine());
        if (!renderer.validate(template.getBodyTemplate())) {
            throw new IllegalArgumentException("Template syntax validation failed");
        }
        
        NotificationTemplateEntity entity = templateMapper.toEntity(template);
        NotificationTemplateEntity saved = templateRepository.save(entity);
        
        log.info("Template created successfully: id={}", saved.getId());
        return templateMapper.toModel(saved);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = TEMPLATE_CACHE, allEntries = true)
    public NotificationTemplate updateTemplate(Long id, NotificationTemplate template) {
        log.info("Updating template: id={}", id);
        
        NotificationTemplateEntity existing = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: id=" + id));
        
        // Validate template if body changed
        if (template.getBodyTemplate() != null && !template.getBodyTemplate().equals(existing.getBodyTemplate())) {
            String engine = template.getEngine() != null ? template.getEngine() : existing.getEngine();
            TemplateRenderer renderer = getRendererForEngine(engine);
            if (!renderer.validate(template.getBodyTemplate())) {
                throw new IllegalArgumentException("Template syntax validation failed");
            }
        }
        
        templateMapper.updateEntity(template, existing);
        NotificationTemplateEntity updated = templateRepository.save(existing);
        
        log.info("Template updated successfully: id={}", id);
        return templateMapper.toModel(updated);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = TEMPLATE_CACHE, allEntries = true)
    public void deleteTemplate(Long id) {
        log.info("Deleting template: id={}", id);
        
        NotificationTemplateEntity template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: id=" + id));
        
        template.setDeleted(Boolean.TRUE);
        template.setActive(Boolean.FALSE);
        templateRepository.save(template);
        
        log.info("Template deleted successfully: id={}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public NotificationTemplate getTemplate(Long id) {
        log.debug("Getting template by id: {}", id);
        
        NotificationTemplateEntity entity = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: id=" + id));
        
        return templateMapper.toModel(entity);
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = TEMPLATE_CACHE, key = "#templateCode + ':' + #locale")
    public NotificationTemplate getTemplate(String templateCode, String locale) {
        log.debug("Getting template: code={}, locale={}", templateCode, locale);
        
        // Try to find template with specified locale
        return templateRepository.findActiveByTemplateCodeAndLocale(templateCode, locale)
            .map(templateMapper::toModel)
            .or(() -> {
                // Fallback to default locale
                log.debug("Template not found for locale {}, falling back to default locale", locale);
                return templateRepository.findActiveByTemplateCodeAndLocale(templateCode, DEFAULT_LOCALE)
                    .map(templateMapper::toModel);
            })
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Template not found: code=%s, locale=%s", templateCode, locale)
            ));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getAllTemplates() {
        log.debug("Getting all templates");
        
        return templateRepository.findAllActive().stream()
            .map(templateMapper::toModel)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplatesByChannel(String channel) {
        log.debug("Getting templates by channel: {}", channel);
        
        return templateRepository.findByChannel(channel).stream()
            .filter(NotificationTemplateEntity::isUsable)
            .map(templateMapper::toModel)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public String renderTemplate(String templateCode, String locale, Map<String, Object> variables) {
        log.debug("Rendering template: code={}, locale={}", templateCode, locale);
        
        NotificationTemplate template = getTemplate(templateCode, locale);
        
        if (!template.isUsable()) {
            throw new IllegalStateException("Template is not active: " + templateCode);
        }
        
        // Merge default variables with provided variables
        Map<String, Object> mergedVariables = new HashMap<>();
        if (template.getDefaultVariables() != null) {
            mergedVariables.putAll(template.getDefaultVariables());
        }
        if (variables != null) {
            mergedVariables.putAll(variables);
        }
        
        // Get appropriate renderer
        TemplateRenderer renderer = getRendererForEngine(template.getEngine());
        
        try {
            String rendered = renderer.render(template.getBodyTemplate(), mergedVariables);
            log.debug("Template rendered successfully: code={}", templateCode);
            return rendered;
        } catch (TemplateRenderingException e) {
            log.error("Failed to render template: code={}, error={}", templateCode, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get the appropriate template renderer for the specified engine
     */
    private TemplateRenderer getRendererForEngine(String engine) {
        return templateRenderers.stream()
            .filter(renderer -> renderer.getEngineType().equalsIgnoreCase(engine))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported template engine: " + engine));
    }
}
