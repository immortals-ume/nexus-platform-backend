package com.immortals.platform.domain.notifications.dto;

import com.immortals.platform.domain.notifications.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for user preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {
    
    private Long id;
    private String userId;
    private Set<Notification.NotificationType> enabledChannels;
    private Set<String> optedOutCategories;
    private Boolean marketingEnabled;
    private Boolean transactionalEnabled;
    private String timezone;
    private QuietHoursResponse quietHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuietHoursResponse {
        private String startTime; // HH:mm format
        private String endTime;
        private Boolean enabled;
    }
}
