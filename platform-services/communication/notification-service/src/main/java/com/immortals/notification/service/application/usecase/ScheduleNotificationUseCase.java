package com.immortals.notification.service.application.usecase;

import com.immortals.platform.domain.notifications.domain.model.Notification;

import java.time.LocalDateTime;

/**
 * Use case for scheduling notifications for future delivery
 * Requirement 13.1, 13.2: Schedule notifications with future delivery timestamp
 */
public interface ScheduleNotificationUseCase {
    
    /**
     * Schedule a notification for future delivery
     * 
     * @param notification The notification to schedule
     * @param scheduledTime The time when the notification should be sent
     * @return The scheduled notification with SCHEDULED status
     */
    Notification schedule(Notification notification, LocalDateTime scheduledTime);
    
    /**
     * Cancel a scheduled notification before it is sent
     * Requirement 13.4: Cancel scheduled notifications
     * 
     * @param notificationId The ID of the notification to cancel
     */
    void cancelScheduled(Long notificationId);
}
