package com.immortals.notification.service.service.impl;

import com.immortals.notification.service.application.usecase.impl.SendNotificationUseCase;
import com.immortals.notification.service.infra.adapter.NotificationRepositoryAdapter;
import com.immortals.notification.service.service.ScheduledNotificationProcessor;
import com.immortals.platform.domain.notifications.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of ScheduledNotificationProcessor
 * Polls for due scheduled notifications and processes them
 * Requirement 13.3: Process notifications when scheduled time arrives
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledNotificationProcessorImpl implements ScheduledNotificationProcessor {

    private final NotificationRepositoryAdapter notificationRepository;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final NotificationMetricsService metricsService;

    /**
     * Scheduled job to process due notifications
     * Runs every minute (configurable via platform.notification.scheduling.poll-interval)
     */
    @Scheduled(fixedDelayString = "${platform.notification.scheduling.poll-interval:60000}")
    @Override
    public void processDueNotifications() {
        log.debug("Checking for due scheduled notifications");

        try {
            LocalDateTime currentTime = LocalDateTime.now();
            List<Notification> dueNotifications = notificationRepository.findDueScheduledNotifications(currentTime);

            if (dueNotifications.isEmpty()) {
                log.debug("No due scheduled notifications found");
                return;
            }

            log.info("Found {} due scheduled notifications to process", dueNotifications.size());

            for (Notification notification : dueNotifications) {
                processScheduledNotification(notification);
            }

            log.info("Completed processing {} scheduled notifications", dueNotifications.size());
        } catch (Exception e) {
            log.error("Error processing scheduled notifications", e);
        }
    }

    /**
     * Process a single scheduled notification
     */
    private void processScheduledNotification(Notification notification) {
        try {
            log.info("Processing scheduled notification: id={}, eventId={}, scheduledAt={}", 
                    notification.getId(), notification.getEventId(), notification.getScheduledAt());

            // Change status from SCHEDULED to PENDING before sending
            notification.setStatus(Notification.NotificationStatus.PENDING);

            // Send the notification using the existing SendNotificationUseCase
            sendNotificationUseCase.execute(notification);
            
            // Record metrics
            metricsService.recordScheduledNotificationProcessed();

            log.info("Successfully processed scheduled notification: id={}, eventId={}", 
                    notification.getId(), notification.getEventId());
        } catch (Exception e) {
            log.error("Failed to process scheduled notification: id={}, eventId={}", 
                    notification.getId(), notification.getEventId(), e);
            
            // Mark as failed
            notification.markAsFailed("Failed to process scheduled notification: " + e.getMessage());
            notificationRepository.save(notification);
        }
    }
}
