package com.immortals.notification.service.controller;

import com.immortals.platform.domain.notifications.dto.TemplatePreviewRequest;
import com.immortals.platform.domain.notifications.dto.TemplatePreviewResponse;
import com.immortals.platform.domain.notifications.dto.TemplateRequest;
import com.immortals.platform.domain.notifications.dto.TemplateResponse;
import com.immortals.notification.service.infra.mapper.TemplateDtoMapper;
import com.immortals.notification.service.application.usecase.ManageTemplateUseCase;
import com.immortals.platform.domain.notifications.domain.NotificationTemplate;
import com.immortals.platform.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for notification template management
 * Provides endpoints for CRUD operations and template preview
 */
@RestController
@RequestMapping("/api/v1/notifications/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Template Management", description = "APIs for managing notification templates")
public class TemplateController {
    
    private final ManageTemplateUseCase manageTemplateUseCase;
    private final TemplateDtoMapper templateDtoMapper;
    
    /**
     * Create a new notification template
     */
    @PostMapping
    @Operation(summary = "Create template", description = "Create a new notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody TemplateRequest request) {
        
        log.info("Creating template: code={}, locale={}", request.getTemplateCode(), request.getLocale());
        
        NotificationTemplate template = templateDtoMapper.toModel(request);
        NotificationTemplate created = manageTemplateUseCase.createTemplate(template);
        TemplateResponse response = templateDtoMapper.toResponse(created);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Template created successfully"));
    }
    
    /**
     * Get all notification templates
     */
    @GetMapping
    @Operation(summary = "Get all templates", description = "Retrieve all notification templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getAllTemplates(
            @Parameter(description = "Filter by channel")
            @RequestParam(required = false) String channel) {
        
        log.info("Getting all templates, channel filter: {}", channel);
        
        List<NotificationTemplate> templates = channel != null
            ? manageTemplateUseCase.getTemplatesByChannel(channel)
            : manageTemplateUseCase.getAllTemplates();
        
        List<TemplateResponse> responses = templateDtoMapper.toResponseList(templates);
        
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    /**
     * Get a specific template by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Retrieve a specific template by its ID")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(
            @Parameter(description = "Template ID")
            @PathVariable Long id) {
        
        log.info("Getting template by id: {}", id);
        
        NotificationTemplate template = manageTemplateUseCase.getTemplate(id);
        TemplateResponse response = templateDtoMapper.toResponse(template);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Update an existing template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Update an existing notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @Parameter(description = "Template ID")
            @PathVariable Long id,
            @Valid @RequestBody TemplateRequest request) {
        
        log.info("Updating template: id={}", id);
        
        NotificationTemplate template = templateDtoMapper.toModel(request);
        NotificationTemplate updated = manageTemplateUseCase.updateTemplate(id, template);
        TemplateResponse response = templateDtoMapper.toResponse(updated);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Template updated successfully"));
    }
    
    /**
     * Delete a template
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", description = "Delete a notification template (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @Parameter(description = "Template ID")
            @PathVariable Long id) {
        
        log.info("Deleting template: id={}", id);
        
        manageTemplateUseCase.deleteTemplate(id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted successfully"));
    }
    
    /**
     * Preview a template with sample variables
     */
    @PostMapping("/{id}/preview")
    @Operation(summary = "Preview template", description = "Preview a template with sample variable substitution")
    public ResponseEntity<ApiResponse<TemplatePreviewResponse>> previewTemplate(
            @Parameter(description = "Template ID")
            @PathVariable Long id,
            @Valid @RequestBody TemplatePreviewRequest request) {
        
        log.info("Previewing template: id={}, locale={}", id, request.getLocale());
        
        NotificationTemplate template = manageTemplateUseCase.getTemplate(id);
        String rendered = manageTemplateUseCase.renderTemplate(
            template.getTemplateCode(),
            request.getLocale(),
            request.getVariables()
        );
        
        TemplatePreviewResponse response = TemplatePreviewResponse.builder()
            .renderedContent(rendered)
            .templateCode(template.getTemplateCode())
            .locale(request.getLocale())
            .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
