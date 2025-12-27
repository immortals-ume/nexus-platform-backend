package com.immortals.notification.service.repository;

import com.immortals.notification.service.model.NotificationCapture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationCapture, Long> {
    
    /**
     * Find notification by event ID
     */
    @Query("SELECT n FROM NotificationCapture n WHERE n.notificationId = :eventId")
    Optional<NotificationCapture> findByEventId(@Param("eventId") String eventId);
}