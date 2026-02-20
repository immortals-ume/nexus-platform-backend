package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.notification.service.application.usecase.port.NotificationRepository;
import com.immortals.notification.service.service.RetrySchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scheduled retry service with exponential backoff
 * Implements Requirements 9.1, 9.2, 9.3, 9.4, 9.5
 * 
 * Retry strategy:
 * - Exponential backoff: 1s, 2s, 4s, 8s, 16s, 32s (configurable)
 * - Max retries: 3 (configurable)
 * - Non-retryable errors: 401, 400, 404
 * - Failover to alternative provider on retry
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RetrySchedulerServiceImpl implements RetrySchedulerService {
    
    private final TaskScheduler taskScheduler;
    private final NotificationRepository notificationRepository;
    private final ProviderRouter providerRouter;
    
    @Value("${platform.notification.retry.max-attempts:3}")
    private int maxRetries;
    
    @Value("${platform.notification.retry.initial-interval:1000}")
    private long initialIntervalMs;
    
    @Value("${platform.notification.retry.multiplier:2.0}")
    private double multiplier;
    
    @Value("${platform.notification.retry.max-interval:10000}")
    private long maxIntervalMs;
    
    // Non-retryable error patterns
    private static final List<Pattern> NON_RETRYABLE_PATTERNS = Arrays.asList(
        Pattern.compile(".*401.*|.*Unauthorized.*|.*Invalid credentials.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*400.*|.*Bad Request.*|.*Invalid.*format.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*404.*|.*Not Found.*|.*does not exist.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*403.*|.*Forbidden.*|.*Access denied.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*Invalid recipient.*|.*Invalid phone number.*|.*Invalid email.*", Pattern.CASE_INSENSITIVE)
    );
    
    @Override
    public void scheduleRetry(Notification notification, boolean attemptFailover) {
        // Check if error is retryable
        if (!isRetryableError(notification.getErrorMessage())) {
            log.warn("Non-retryable error for notification: {}, error: {}", 
                    notification.getEventId(), notification.getErrorMessage());
            markAsPermanentlyFailed(notification, "Non-retryable error: " + notification.getErrorMessage());
            return;
        }
        
        // Check if max retries reached
        if (!shouldRetry(notification)) {
            log.info("Max retries reached for notification: {}", notification.getEventId());
            markAsPermanentlyFailed(notification, "Max retries exceeded");
            return;
        }
        
        // Calculate backoff delay
        var retryCount = notification.getRetryCount();
        var delaySeconds = calculateBackoffDelay(retryCount);
        var scheduledTime = Instant.now().plusSeconds(delaySeconds);
        
        log.info("Scheduling retry {} for notification {} at {} (delay: {}s, failover: {})", 
                 retryCount + 1, notification.getEventId(), scheduledTime, delaySeconds, attemptFailover);
        
        // Schedule retry task
        taskScheduler.schedule(
            () -> retryNotification(notification, attemptFailover),
            scheduledTime
        );
    }
    
    @Override
    public long calculateBackoffDelay(int retryCount) {
        // Exponential backoff: initialInterval * (multiplier ^ retryCount)
        long delayMs = (long) (initialIntervalMs * Math.pow(multiplier, retryCount));
        
        // Cap at max interval
        delayMs = Math.min(delayMs, maxIntervalMs);
        
        // Convert to seconds
        return delayMs / 1000;
    }
    
    @Override
    public boolean shouldRetry(Notification notification) {
        var maxRetryCount = notification.getMaxRetries() != null 
            ? notification.getMaxRetries() 
            : maxRetries;
        
        return notification.getRetryCount() < maxRetryCount;
    }
    
    @Override
    public boolean isRetryableError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return true; // Unknown errors are retryable
        }
        
        // Check against non-retryable patterns
        for (Pattern pattern : NON_RETRYABLE_PATTERNS) {
            if (pattern.matcher(errorMessage).matches()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void markAsPermanentlyFailed(Notification notification, String reason) {
        notification.markAsFailed(reason);
        notification.setDeliveryStatus(Notification.DeliveryStatus.FAILED);
        notificationRepository.save(notification);
        
        log.error("Notification permanently failed: eventId={}, reason={}", 
                notification.getEventId(), reason);
    }
    
    /**
     * Retry notification with optional failover to alternative provider
     * Requirement 9.5: Retry with failover to alternative provider
     */
    private void retryNotification(Notification notification, boolean attemptFailover) {
        try {
            log.info("Retrying notification: {}, attempt: {}, failover: {}", 
                     notification.getEventId(), notification.getRetryCount() + 1, attemptFailover);
            
            // Increment retry count and update timestamp
            notification.incrementRetryCount();
            notification.setProcessedAt(LocalDateTime.now());
            
            // If failover requested, try to get alternative provider
            if (attemptFailover && notification.getProviderId() != null) {
                var alternativeProvider = providerRouter.getFailoverProvider(
                        notification.getType(),
                        notification.getCountryCode(),
                        notification.getProviderId()
                );
                
                if (alternativeProvider.isPresent()) {
                    log.info("Attempting failover to provider: {} for notification: {}", 
                            alternativeProvider.get().getProviderId(), notification.getEventId());
                    
                    // Send via alternative provider
                    var success = alternativeProvider.get().send(notification);
                    
                    if (success) {
                        notification.setProviderId(alternativeProvider.get().getProviderId());
                        notification.markAsSent();
                        notification.setDeliveryStatus(Notification.DeliveryStatus.SENT);
                        notificationRepository.save(notification);
                        log.info("Notification sent successfully via failover provider: {}", 
                                alternativeProvider.get().getProviderId());
                        return;
                    } else {
                        log.warn("Failover provider also failed for notification: {}", 
                                notification.getEventId());
                    }
                } else {
                    log.warn("No alternative provider available for failover: channel={}, country={}", 
                            notification.getType(), notification.getCountryCode());
                }
            }
            
            // Try original provider again
            var provider = providerRouter.selectProvider(
                    notification.getType(),
                    notification.getCountryCode(),
                    notification.getPriority()
            );
            
            if (provider.isEmpty()) {
                log.error("No provider available for retry: channel={}, country={}", 
                        notification.getType(), notification.getCountryCode());
                markAsPermanentlyFailed(notification, "No provider available");
                return;
            }
            
            var success = provider.get().send(notification);
            
            if (success) {
                notification.setProviderId(provider.get().getProviderId());
                notification.markAsSent();
                notification.setDeliveryStatus(Notification.DeliveryStatus.SENT);
                notificationRepository.save(notification);
                log.info("Notification sent successfully on retry: {}", notification.getEventId());
            } else {
                notification.markAsFailed("Provider failed on retry");
                notificationRepository.save(notification);
                
                // Schedule next retry with failover
                scheduleRetry(notification, true);
            }
            
        } catch (Exception e) {
            log.error("Retry failed for notification: {}", notification.getEventId(), e);
            notification.markAsFailed("Retry exception: " + e.getMessage());
            notificationRepository.save(notification);
            
            // Schedule next retry with failover
            scheduleRetry(notification, true);
        }
    }
}
