package com.immortals.notification.service.repository;

import com.immortals.platform.domain.notifications.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationLog entity
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {
    
    /**
     * Find notification log by event ID for idempotency check
     */
    Optional<NotificationLog> findByEventId(String eventId);
    
    /**
     * Check if notification with event ID already exists
     */
    boolean existsByEventId(String eventId);
    
    /**
     * Find all scheduled notifications that are due for processing
     * Requirement 13.3: Process notifications when scheduled time arrives
     */
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'SCHEDULED' AND n.scheduledAt <= :currentTime")
    List<NotificationLog> findDueScheduledNotifications(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find all scheduled notifications (for listing)
     * Requirement 13.5: List pending scheduled notifications
     */
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'SCHEDULED' ORDER BY n.scheduledAt ASC")
    List<NotificationLog> findAllScheduledNotifications();
    
    /**
     * Find notifications with pagination and filtering
     * Requirement 11.3: List notifications with search and filter
     */
    @Query("SELECT n FROM NotificationLog n WHERE " +
           "(:status IS NULL OR n.status = :status) AND " +
           "(:notificationType IS NULL OR n.notificationType = :notificationType) AND " +
           "(:recipient IS NULL OR LOWER(n.recipient) LIKE LOWER(CONCAT('%', :recipient, '%'))) AND " +
           "(:startDate IS NULL OR n.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR n.createdAt <= :endDate)")
    Page<NotificationLog> findWithFilters(
        @Param("status") String status,
        @Param("notificationType") String notificationType,
        @Param("recipient") String recipient,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}
