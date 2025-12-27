package com.immortals.platform.domain.notifications.dto;

import com.immortals.platform.domain.notifications.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Filter criteria for analytics queries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFilter {
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Notification.NotificationType channel;
    private String providerId;
    private String countryCode;
    
    public boolean hasChannel() {
        return channel != null;
    }
    
    public boolean hasProvider() {
        return providerId != null && !providerId.isBlank();
    }
    
    public boolean hasCountry() {
        return countryCode != null && !countryCode.isBlank();
    }
    
    public boolean hasDateRange() {
        return startDate != null && endDate != null;
    }
}
