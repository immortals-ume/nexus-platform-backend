package com.immortals.notification.service.application.usecase.port;

import com.immortals.platform.domain.notifications.domain.Notification;

import java.util.Optional;

/**
 * Port interface for notification persistence (Hexagonal Architecture)
 */
public interface NotificationRepository {
    
    /**
     * Save a notification
     */
    Notification save(Notification notification);
    
    /**
     * Find notification by event ID
     */
    Optional<Notification> findByEventId(String eventId);
    
    /**
     * Check if notification exists by event ID
     */
    boolean existsByEventId(String eventId);
}
