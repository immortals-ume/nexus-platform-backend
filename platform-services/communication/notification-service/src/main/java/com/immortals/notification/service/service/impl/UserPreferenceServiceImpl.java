package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.model.Notification;
import com.immortals.platform.domain.notifications.domain.model.UserPreference;
import com.immortals.platform.domain.notifications.entity.UserPreferenceEntity;
import com.immortals.notification.service.repository.UserPreferenceRepository;
import com.immortals.notification.service.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of UserPreferenceService with caching support
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {
    
    private final UserPreferenceRepository userPreferenceRepository;
    
    @Override
    @Cacheable(value = "userPreferences", key = "#userId")
    public Optional<UserPreference> getUserPreference(String userId, Notification.NotificationType channel) {
        log.debug("Fetching user preference for user: {}", userId);
        
        return userPreferenceRepository.findByUserId(userId)
                .map(this::mapToDomain);
    }
    
    @Override
    @Cacheable(value = "userPreferences", key = "#userId")
    public Optional<UserPreference> getUserPreferences(String userId) {
        log.debug("Fetching all user preferences for user: {}", userId);
        
        return userPreferenceRepository.findByUserId(userId)
                .map(this::mapToDomain);
    }
    
    @Override
    @CacheEvict(value = "userPreferences", key = "#userId")
    public UserPreference updatePreferences(String userId, UserPreference userPreference) {
        log.info("Updating user preferences for user: {}", userId);
        
        // Set the userId to ensure consistency
        userPreference.setUserId(userId);
        
        // Find existing preference or create new
        Optional<UserPreferenceEntity> existingOpt = userPreferenceRepository.findByUserId(userId);
        
        UserPreferenceEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            // Update existing entity
            updateEntityFromDomain(entity, userPreference);
        } else {
            // Create new entity
            entity = mapToEntity(userPreference);
        }
        
        UserPreferenceEntity saved = userPreferenceRepository.save(entity);
        log.info("User preferences updated for user: {}", userId);
        
        return mapToDomain(saved);
    }
    
    @Override
    public boolean isChannelEnabled(String userId, Notification.NotificationType channel) {
        try {
            Optional<UserPreference> preferenceOpt = getUserPreference(userId, channel);
            return preferenceOpt
                    .map(pref -> pref.isChannelEnabled(channel))
                    .orElse(true); // Default to enabled if no preference set
        } catch (Exception e) {
            log.warn("Error checking channel preference for user: {}, defaulting to enabled", userId, e);
            return true; // Default to enabled on error
        }
    }
    
    @Override
    public boolean shouldSendNotification(String userId, Notification.NotificationType channel) {
        try {
            Optional<UserPreference> preferenceOpt = getUserPreference(userId, channel);
            
            if (preferenceOpt.isEmpty()) {
                // No preference set, allow notification
                return true;
            }
            
            UserPreference preference = preferenceOpt.get();
            
            // Check if channel is enabled
            if (!preference.isChannelEnabled(channel)) {
                log.info("Notification skipped - channel disabled for user: {}, channel: {}", userId, channel);
                return false;
            }
            
            // Check quiet hours
            if (isInQuietHours(preference)) {
                log.info("Notification skipped - quiet hours for user: {}, channel: {}", userId, channel);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.warn("Error checking user preferences for user: {}, defaulting to allow", userId, e);
            return true; // Default to allow on error
        }
    }
    
    @Override
    @CacheEvict(value = "userPreferences", key = "#userPreference.userId")
    public UserPreference savePreference(UserPreference userPreference) {
        UserPreferenceEntity entity = mapToEntity(userPreference);
        UserPreferenceEntity saved = userPreferenceRepository.save(entity);
        log.info("User preference saved for user: {}", userPreference.getUserId());
        return mapToDomain(saved);
    }
    
    private boolean isInQuietHours(UserPreference preference) {
        if (preference.getQuietHours() == null || !preference.getQuietHours().isEnabled()) {
            return false;
        }
        
        UserPreference.QuietHours quietHours = preference.getQuietHours();
        if (quietHours.getStartTime() == null || quietHours.getEndTime() == null) {
            return false;
        }
        
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(quietHours.getStartTime());
        LocalTime end = LocalTime.parse(quietHours.getEndTime());
        
        // Handle quiet hours that span midnight
        if (start.isBefore(end)) {
            return now.isAfter(start) && now.isBefore(end);
        } else {
            return now.isAfter(start) || now.isBefore(end);
        }
    }
    
    private UserPreference mapToDomain(UserPreferenceEntity entity) {
        // Parse enabled channels from comma-separated string
        Set<Notification.NotificationType> enabledChannels = new HashSet<>();
        if (entity.getEnabledChannels() != null && !entity.getEnabledChannels().isEmpty()) {
            enabledChannels = Arrays.stream(entity.getEnabledChannels().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Notification.NotificationType::valueOf)
                    .collect(Collectors.toSet());
        }
        
        // Parse opted out categories from comma-separated string
        Set<String> optedOutCategories = new HashSet<>();
        if (entity.getOptedOutCategories() != null && !entity.getOptedOutCategories().isEmpty()) {
            optedOutCategories = Arrays.stream(entity.getOptedOutCategories().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        
        // Build quiet hours
        UserPreference.QuietHours quietHours = null;
        if (entity.isQuietHoursEnabled()) {
            quietHours = UserPreference.QuietHours.builder()
                    .startTime(entity.getQuietHoursStart())
                    .endTime(entity.getQuietHoursEnd())
                    .enabled(entity.isQuietHoursEnabled())
                    .build();
        }
        
        return UserPreference.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .enabledChannels(enabledChannels)
                .optedOutCategories(optedOutCategories)
                .marketingEnabled(entity.isMarketingEnabled())
                .transactionalEnabled(entity.isTransactionalEnabled())
                .timezone(entity.getTimezone())
                .quietHours(quietHours)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private UserPreferenceEntity mapToEntity(UserPreference domain) {
        UserPreferenceEntity entity = new UserPreferenceEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        
        // Convert enabled channels set to comma-separated string
        if (domain.getEnabledChannels() != null && !domain.getEnabledChannels().isEmpty()) {
            String channelsStr = domain.getEnabledChannels().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
            entity.setEnabledChannels(channelsStr);
        }
        
        // Convert opted out categories set to comma-separated string
        if (domain.getOptedOutCategories() != null && !domain.getOptedOutCategories().isEmpty()) {
            String categoriesStr = String.join(",", domain.getOptedOutCategories());
            entity.setOptedOutCategories(categoriesStr);
        }
        
        entity.setMarketingEnabled(domain.isMarketingEnabled());
        entity.setTransactionalEnabled(domain.isTransactionalEnabled());
        entity.setTimezone(domain.getTimezone());
        
        // Map quiet hours
        if (domain.getQuietHours() != null) {
            entity.setQuietHoursStart(domain.getQuietHours().getStartTime());
            entity.setQuietHoursEnd(domain.getQuietHours().getEndTime());
            entity.setQuietHoursEnabled(domain.getQuietHours().isEnabled());
        }
        
        return entity;
    }
    
    private void updateEntityFromDomain(UserPreferenceEntity entity, UserPreference domain) {
        // Convert enabled channels set to comma-separated string
        if (domain.getEnabledChannels() != null && !domain.getEnabledChannels().isEmpty()) {
            String channelsStr = domain.getEnabledChannels().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
            entity.setEnabledChannels(channelsStr);
        } else {
            entity.setEnabledChannels(null);
        }
        
        // Convert opted out categories set to comma-separated string
        if (domain.getOptedOutCategories() != null && !domain.getOptedOutCategories().isEmpty()) {
            String categoriesStr = String.join(",", domain.getOptedOutCategories());
            entity.setOptedOutCategories(categoriesStr);
        } else {
            entity.setOptedOutCategories(null);
        }
        
        entity.setMarketingEnabled(domain.isMarketingEnabled());
        entity.setTransactionalEnabled(domain.isTransactionalEnabled());
        entity.setTimezone(domain.getTimezone());
        
        // Map quiet hours
        if (domain.getQuietHours() != null) {
            entity.setQuietHoursStart(domain.getQuietHours().getStartTime());
            entity.setQuietHoursEnd(domain.getQuietHours().getEndTime());
            entity.setQuietHoursEnabled(domain.getQuietHours().isEnabled());
        } else {
            entity.setQuietHoursStart(null);
            entity.setQuietHoursEnd(null);
            entity.setQuietHoursEnabled(false);
        }
    }
}
