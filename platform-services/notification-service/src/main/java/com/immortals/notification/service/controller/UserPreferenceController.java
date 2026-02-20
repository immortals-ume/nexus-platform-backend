package com.immortals.notification.service.controller;

import com.immortals.platform.domain.notifications.domain.UserPreference;
import com.immortals.platform.domain.notifications.dto.UserPreferenceRequest;
import com.immortals.platform.domain.notifications.dto.UserPreferenceResponse;
import com.immortals.notification.service.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for user notification preferences
 * Provides endpoints for managing user preferences
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceController {
    
    private final UserPreferenceService userPreferenceService;
    
    /**
     * Get user preferences
     * GET /api/v1/notifications/preferences/{userId}
     * 
     * @param userId the user ID
     * @return user preferences or 404 if not found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserPreferenceResponse> getUserPreferences(@PathVariable String userId) {
        log.info("Getting user preferences for userId: {}", userId);
        
        return userPreferenceService.getUserPreferences(userId)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update user preferences
     * PUT /api/v1/notifications/preferences/{userId}
     * 
     * @param userId the user ID
     * @param request the preference update request
     * @return updated user preferences
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserPreferenceResponse> updateUserPreferences(
            @PathVariable String userId,
            @Valid @RequestBody UserPreferenceRequest request) {
        
        log.info("Updating user preferences for userId: {}", userId);
        
        UserPreference preference = mapToDomain(request);
        UserPreference updated = userPreferenceService.updatePreferences(userId, preference);
        
        return ResponseEntity.ok(mapToResponse(updated));
    }
    
    /**
     * Map domain model to response DTO
     */
    private UserPreferenceResponse mapToResponse(UserPreference domain) {
        UserPreferenceResponse.QuietHoursResponse quietHoursResponse = null;
        if (domain.getQuietHours() != null) {
            quietHoursResponse = UserPreferenceResponse.QuietHoursResponse.builder()
                    .startTime(domain.getQuietHours().getStartTime())
                    .endTime(domain.getQuietHours().getEndTime())
                    .enabled(domain.getQuietHours().isEnabled())
                    .build();
        }
        
        return UserPreferenceResponse.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .enabledChannels(domain.getEnabledChannels())
                .optedOutCategories(domain.getOptedOutCategories())
                .marketingEnabled(domain.isMarketingEnabled())
                .transactionalEnabled(domain.isTransactionalEnabled())
                .timezone(domain.getTimezone())
                .quietHours(quietHoursResponse)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
    
    /**
     * Map request DTO to domain model
     */
    private UserPreference mapToDomain(UserPreferenceRequest request) {
        UserPreference.QuietHours quietHours = null;
        if (request.getQuietHours() != null) {
            quietHours = UserPreference.QuietHours.builder()
                    .startTime(request.getQuietHours().getStartTime())
                    .endTime(request.getQuietHours().getEndTime())
                    .enabled(request.getQuietHours().getEnabled() != null ? request.getQuietHours().getEnabled() : false)
                    .build();
        }
        
        return UserPreference.builder()
                .enabledChannels(request.getEnabledChannels())
                .optedOutCategories(request.getOptedOutCategories())
                .marketingEnabled(request.getMarketingEnabled() != null ? request.getMarketingEnabled() : true)
                .transactionalEnabled(request.getTransactionalEnabled() != null ? request.getTransactionalEnabled() : true)
                .timezone(request.getTimezone())
                .quietHours(quietHours)
                .build();
    }
}
