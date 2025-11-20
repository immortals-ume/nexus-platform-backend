package com.immortals.notificationservice.service.impl;

import com.immortals.notificationservice.application.usecase.SendNotificationUseCase;
import com.immortals.notificationservice.domain.model.Notification;
import com.immortals.notificationservice.service.RetrySchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled retry service with exponential backoff
 * Retries: 1min, 5min, 15min, 1hr, 6hr, 24hr
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RetrySchedulerServiceImpl implements RetrySchedulerService {
    
    private final TaskScheduler taskScheduler;
    private final SendNotificationUseCase sendNotificationUseCase;
    
    private static final int MAX_RETRIES = 6;
    private static final long[] BACKOFF_DELAYS = {
        60,      // 1 minute
        300,     // 5 minutes
        900,     // 15 minutes
        3600,    // 1 hour
        21600,   // 6 hours
        86400    // 24 hours
    };
    
    @Override
    public void scheduleRetry(Notification notification) {
        if (!shouldRetry(notification)) {
            log.info("Max retries reached for notification: {}", notification.getEventId());
            return;
        }
        
        var retryCount = notification.getRetryCount();
        var delaySeconds = calculateBackoffDelay(retryCount);
        var scheduledTime = Instant.now().plusSeconds(delaySeconds);
        
        log.info("Scheduling retry {} for notification {} at {}", 
                 retryCount + 1, notification.getEventId(), scheduledTime);
        
        taskScheduler.schedule(
            () -> retryNotification(notification),
            scheduledTime
        );
    }
    
    @Override
    public long calculateBackoffDelay(int retryCount) {
        if (retryCount >= BACKOFF_DELAYS.length) {
            return BACKOFF_DELAYS[BACKOFF_DELAYS.length - 1];
        }
        return BACKOFF_DELAYS[retryCount];
    }
    
    @Override
    public boolean shouldRetry(Notification notification) {
        var maxRetries = notification.getMaxRetries() != null 
            ? notification.getMaxRetries() 
            : MAX_RETRIES;
        
        return notification.getRetryCount() < maxRetries;
    }
    
    private void retryNotification(Notification notification) {
        try {
            log.info("Retrying notification: {}, attempt: {}", 
                     notification.getEventId(), notification.getRetryCount() + 1);
            
            notification.incrementRetryCount();
            sendNotificationUseCase.execute(notification);
            
        } catch (Exception e) {
            log.error("Retry failed for notification: {}", notification.getEventId(), e);
            scheduleRetry(notification); // Schedule next retry
        }
    }
}
