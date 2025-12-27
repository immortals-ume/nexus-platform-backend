package com.immortals.platform.domain.notifications.dto;

import com.immortals.platform.domain.notifications.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for updating user preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceRequest {
    
    private Set<Notification.NotificationType> enabledChannels;
    private Set<String> optedOutCategories;
    private Boolean marketingEnabled;
    private Boolean transactionalEnabled;
    private String timezone;
    private QuietHoursRequest quietHours;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuietHoursRequest {
        private String startTime; // HH:mm format
        private String endTime;
        private Boolean enabled;
    }
}
