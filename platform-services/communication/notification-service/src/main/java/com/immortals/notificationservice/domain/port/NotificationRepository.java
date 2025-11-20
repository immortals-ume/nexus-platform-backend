package com.immortals.notificationservice.domain.port;

import com.immortals.notificationservice.domain.model.Notification;

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
