package com.immortals.notification.service.service;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.UserPreference;

import java.util.Optional;

/**
 * Service for managing user notification preferences
 */
public interface UserPreferenceService {
    
    /**
     * Get user preferences for a specific channel
     * 
     * @param userId the user ID
     * @param channel the notification channel
     * @return user preference if exists
     */
    Optional<UserPreference> getUserPreference(String userId, Notification.NotificationType channel);
    
    /**
     * Get all user preferences for a user
     * 
     * @param userId the user ID
     * @return user preference if exists
     */
    Optional<UserPreference> getUserPreferences(String userId);
    
    /**
     * Update user preferences
     * 
     * @param userId the user ID
     * @param userPreference the updated preferences
     * @return updated user preference
     */
    UserPreference updatePreferences(String userId, UserPreference userPreference);
    
    /**
     * Check if user has enabled notifications for a channel
     * 
     * @param userId the user ID
     * @param channel the notification channel
     * @return true if enabled, false if disabled
     */
    boolean isChannelEnabled(String userId, Notification.NotificationType channel);
    
    /**
     * Check if notification should be sent based on user preferences
     * Includes quiet hours and frequency limits
     * 
     * @param userId the user ID
     * @param channel the notification channel
     * @return true if notification should be sent, false if should be skipped
     */
    boolean shouldSendNotification(String userId, Notification.NotificationType channel);
    
    /**
     * Save or update user preference
     * 
     * @param userPreference the user preference to save
     * @return saved user preference
     */
    UserPreference savePreference(UserPreference userPreference);
}
