package com.immortals.notificationservice.repository;

import com.immortals.notificationservice.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for NotificationLog entity
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    
    /**
     * Find notification log by event ID for idempotency check
     */
    Optional<NotificationLog> findByEventId(String eventId);
    
    /**
     * Check if notification with event ID already exists
     */
    boolean existsByEventId(String eventId);
}
