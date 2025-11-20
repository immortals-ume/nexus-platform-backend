package com.immortals.notificationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User notification preferences
 * Manages opt-in/opt-out settings per channel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    
    private Long id;
    private String userId;
    private Set<Notification.NotificationType> enabledChannels;
    private Set<String> optedOutCategories;
    private boolean marketingEnabled;
    private boolean transactionalEnabled;
    private String timezone;
    private QuietHours quietHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuietHours {
        private String startTime; // HH:mm format
        private String endTime;
        private boolean enabled;
    }
    
    public boolean isChannelEnabled(Notification.NotificationType type) {
        return enabledChannels != null && enabledChannels.contains(type);
    }
    
    public boolean isCategoryOptedOut(String category) {
        return optedOutCategories != null && optedOutCategories.contains(category);
    }
    
    public void optOut(Notification.NotificationType type) {
        if (enabledChannels == null) {
            enabledChannels = new HashSet<>();
        }
        enabledChannels.remove(type);
    }
    
    public void optIn(Notification.NotificationType type) {
        if (enabledChannels == null) {
            enabledChannels = new HashSet<>();
        }
        enabledChannels.add(type);
    }
}
