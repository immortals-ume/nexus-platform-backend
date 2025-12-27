package com.immortals.notification.service.application.usecase.impl;

import com.immortals.notification.service.application.usecase.ScheduleNotificationUseCase;
import com.immortals.platform.common.exception.BusinessException;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import com.immortals.platform.domain.notifications.domain.model.Notification;
import com.immortals.platform.domain.notifications.domain.model.NotificationPriority;
import com.immortals.platform.domain.notifications.domain.port.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of ScheduleNotificationUseCase
 * Handles scheduling notifications for future delivery
 * Requirements: 13.1, 13.2, 13.4
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleNotificationUseCaseImpl implements ScheduleNotificationUseCase {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification schedule(Notification notification, LocalDateTime scheduledTime) {
        log.info("Scheduling notification: eventId={}, scheduledTime={}", 
                notification.getEventId(), scheduledTime);

        // Validate scheduled time is in the future
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Scheduled time must be in the future");
        }

        // Check for duplicate event ID
        if (notificationRepository.existsByEventId(notification.getEventId())) {
            log.warn("Notification with eventId already exists: {}", notification.getEventId());
            return notificationRepository.findByEventId(notification.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", notification.getEventId()));
        }

        // Initialize notification fields
        initializeNotification(notification, scheduledTime);

        // Save notification with SCHEDULED status
        Notification savedNotification = notificationRepository.save(notification);
        
        log.info("Notification scheduled successfully: id={}, eventId={}, scheduledAt={}", 
                savedNotification.getId(), savedNotification.getEventId(), scheduledTime);

        return savedNotification;
    }

    @Override
    @Transactional
    public void cancelScheduled(Long notificationId) {
        log.info("Cancelling scheduled notification: id={}", notificationId);

        // Find the notification
        Notification notification = notificationRepository.findByEventId(String.valueOf(notificationId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        // Verify it's in SCHEDULED status
        if (notification.getStatus() != Notification.NotificationStatus.SCHEDULED) {
            throw new BusinessException(
                    String.format("Cannot cancel notification with status: %s. Only SCHEDULED notifications can be cancelled.", 
                            notification.getStatus()));
        }

        // Update status to CANCELLED
        notification.setStatus(Notification.NotificationStatus.CANCELLED);
        notification.setProcessedAt(LocalDateTime.now());
        notification.setErrorMessage("Cancelled by user");

        notificationRepository.save(notification);

        log.info("Notification cancelled successfully: id={}, eventId={}", 
                notificationId, notification.getEventId());
    }

    /**
     * Initialize notification with default values and SCHEDULED status
     */
    private void initializeNotification(Notification notification, LocalDateTime scheduledTime) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        
        notification.setScheduledAt(scheduledTime);
        notification.setStatus(Notification.NotificationStatus.SCHEDULED);
        
        if (notification.getRetryCount() == null) {
            notification.setRetryCount(0);
        }
        
        if (notification.getMaxRetries() == null) {
            notification.setMaxRetries(3);
        }
        
        if (notification.getPriority() == null) {
            notification.setPriority(NotificationPriority.NORMAL);
        }
        
        if (notification.getDeliveryStatus() == null) {
            notification.setDeliveryStatus(Notification.DeliveryStatus.PENDING);
        }
    }
}
