package com.immortals.notification.service.application.usecase;

import com.immortals.platform.domain.notifications.domain.model.Notification;
import com.immortals.platform.domain.notifications.domain.model.NotificationPriority;
import com.immortals.platform.domain.notifications.domain.port.NotificationProvider;
import com.immortals.platform.domain.notifications.domain.port.NotificationRepository;
import com.immortals.notification.service.service.*;
import com.immortals.notification.service.service.impl.CountryCodeExtractor;
import com.immortals.notification.service.service.impl.ProviderRouter;
import com.immortals.notificationservice.service.*;
import com.immortals.platform.common.exception.BusinessException;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Use case for sending notifications
 * Implements hexagonal architecture application layer with enhanced features:
 * - Provider routing based on channel, country, and health
 * - Template rendering with variable substitution
 * - Country code extraction from recipient
 * - User preference checking
 * - Rate limiting
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SendNotificationUseCase {
    
    private final NotificationRepository notificationRepository;
    private final ProviderRouter providerRouter;
    private final TemplateService templateService;
    private final CountryCodeExtractor countryCodeExtractor;
    private final UserPreferenceService userPreferenceService;
    private final RateLimitService rateLimitService;
    private final DeduplicationService deduplicationService;
    private final RetrySchedulerService retrySchedulerService;
    
    @Transactional
    public Notification execute(Notification notification) {
        log.info("Processing notification: eventId={}, type={}, recipient={}", 
                 notification.getEventId(), notification.getType(), notification.getRecipient());
        
        // Idempotency check using repository
        if (notificationRepository.existsByEventId(notification.getEventId())) {
            log.info("Notification already processed (repository check): eventId={}", notification.getEventId());
            return notificationRepository.findByEventId(notification.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", notification.getEventId()));
        }
        
        // Additional deduplication check using cache
        if (deduplicationService.isDuplicate(notification)) {
            log.info("Notification is duplicate (cache check): eventId={}", notification.getEventId());
            return notificationRepository.findByEventId(notification.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", notification.getEventId()));
        }
        
        // Initialize notification
        initializeNotification(notification);
        
        // Extract country code from recipient
        extractCountryCode(notification);
        
        // Check user preferences
        if (!checkUserPreferences(notification)) {
            notification.markAsFailed("User preferences do not allow this notification");
            return notificationRepository.save(notification);
        }
        
        // Check rate limiting
        if (!checkRateLimit(notification)) {
            notification.markAsFailed("Rate limit exceeded");
            return notificationRepository.save(notification);
        }
        
        // Render template if template code is provided
        renderTemplate(notification);
        
        // Save as pending
        var savedNotification = notificationRepository.save(notification);
        
        // Mark as sent in deduplication cache
        deduplicationService.markAsSent(savedNotification);
        
        // Route to appropriate provider
        var provider = routeToProvider(savedNotification);
        
        // Send notification
        var success = provider.send(savedNotification);
        
        // Update status
        updateNotificationStatus(savedNotification, success, provider.getProviderId());
        
        // Consume rate limit token
        if (success) {
            consumeRateLimitToken(savedNotification);
        }
        
        return notificationRepository.save(savedNotification);
    }
    
    private void initializeNotification(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        if (notification.getStatus() == null) {
            notification.setStatus(Notification.NotificationStatus.PENDING);
        }
        if (notification.getRetryCount() == null) {
            notification.setRetryCount(0);
        }
        if (notification.getPriority() == null) {
            notification.setPriority(NotificationPriority.NORMAL);
        }
        if (notification.getDeliveryStatus() == null) {
            notification.setDeliveryStatus(Notification.DeliveryStatus.PENDING);
        }
    }
    
    /**
     * Extract country code from recipient based on notification type
     * Requirement 5.2: Extract country code from recipient
     */
    private void extractCountryCode(Notification notification) {
        if (notification.getCountryCode() == null || notification.getCountryCode().isBlank()) {
            String countryCode = countryCodeExtractor.extractCountryCode(
                    notification.getRecipient(), 
                    notification.getType()
            );
            notification.setCountryCode(countryCode);
            log.debug("Extracted country code: {} for recipient: {}", countryCode, notification.getRecipient());
        }
    }
    
    /**
     * Check user preferences to determine if notification should be sent
     * Requirement 7.2: Check user preferences and skip disabled channels
     * Requirement 7.3: Delay notifications during quiet hours
     */
    private boolean checkUserPreferences(Notification notification) {
        // Extract user ID from metadata or use recipient as fallback
        String userId = extractUserId(notification);
        
        // Check if channel is enabled
        if (!userPreferenceService.isChannelEnabled(userId, notification.getType())) {
            log.info("Notification blocked - channel disabled: userId={}, channel={}", 
                    userId, notification.getType());
            return false;
        }
        
        // Check quiet hours and delay if necessary
        if (isInQuietHours(userId)) {
            LocalDateTime delayUntil = calculateQuietHoursEnd(userId);
            if (delayUntil != null) {
                notification.setScheduledAt(delayUntil);
                notification.setStatus(Notification.NotificationStatus.SCHEDULED);
                log.info("Notification delayed due to quiet hours: userId={}, delayUntil={}", 
                        userId, delayUntil);
                // Save as scheduled and return false to skip immediate sending
                notificationRepository.save(notification);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if current time is within user's quiet hours
     */
    private boolean isInQuietHours(String userId) {
        try {
            var preferenceOpt = userPreferenceService.getUserPreferences(userId);
            if (preferenceOpt.isEmpty()) {
                return false;
            }
            
            var preference = preferenceOpt.get();
            if (preference.getQuietHours() == null || !preference.getQuietHours().isEnabled()) {
                return false;
            }
            
            var quietHours = preference.getQuietHours();
            if (quietHours.getStartTime() == null || quietHours.getEndTime() == null) {
                return false;
            }
            
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime start = java.time.LocalTime.parse(quietHours.getStartTime());
            java.time.LocalTime end = java.time.LocalTime.parse(quietHours.getEndTime());
            
            // Handle quiet hours that span midnight
            if (start.isBefore(end)) {
                return now.isAfter(start) && now.isBefore(end);
            } else {
                return now.isAfter(start) || now.isBefore(end);
            }
        } catch (Exception e) {
            log.warn("Error checking quiet hours for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Calculate when quiet hours end to schedule notification
     */
    private LocalDateTime calculateQuietHoursEnd(String userId) {
        try {
            var preferenceOpt = userPreferenceService.getUserPreferences(userId);
            if (preferenceOpt.isEmpty()) {
                return null;
            }
            
            var preference = preferenceOpt.get();
            if (preference.getQuietHours() == null || !preference.getQuietHours().isEnabled()) {
                return null;
            }
            
            var quietHours = preference.getQuietHours();
            if (quietHours.getEndTime() == null) {
                return null;
            }
            
            java.time.LocalTime endTime = java.time.LocalTime.parse(quietHours.getEndTime());
            java.time.LocalTime now = java.time.LocalTime.now();
            
            LocalDateTime scheduledTime;
            if (endTime.isAfter(now)) {
                // Quiet hours end today
                scheduledTime = LocalDateTime.of(LocalDateTime.now().toLocalDate(), endTime);
            } else {
                // Quiet hours end tomorrow
                scheduledTime = LocalDateTime.of(LocalDateTime.now().toLocalDate().plusDays(1), endTime);
            }
            
            return scheduledTime;
        } catch (Exception e) {
            log.warn("Error calculating quiet hours end for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Check rate limiting before sending notification
     * Requirement 8.1: Enforce rate limits per user per channel
     */
    private boolean checkRateLimit(Notification notification) {
        String userId = extractUserId(notification);
        
        boolean withinLimit = rateLimitService.isWithinRateLimit(userId, notification.getType());
        
        if (!withinLimit) {
            log.warn("Rate limit exceeded for user: {}, channel: {}", userId, notification.getType());
        }
        
        return withinLimit;
    }
    
    /**
     * Consume rate limit token after successful send
     */
    private void consumeRateLimitToken(Notification notification) {
        String userId = extractUserId(notification);
        rateLimitService.consumeToken(userId, notification.getType());
    }
    
    /**
     * Render template with variables if template code is provided
     * Requirement 4.4: Render template with provided variables
     */
    private void renderTemplate(Notification notification) {
        if (notification.getTemplateCode() != null && !notification.getTemplateCode().isBlank()) {
            try {
                Map<String, Object> variables = notification.getTemplateVariables();
                
                // Render message template
                if (templateService.templateExists(notification.getTemplateCode())) {
                    String renderedMessage = templateService.renderTemplate(
                            notification.getTemplateCode(), 
                            variables
                    );
                    notification.setMessage(renderedMessage);
                    
                    // Render HTML template if available
                    try {
                        String renderedHtml = templateService.renderHtmlTemplate(
                                notification.getTemplateCode(), 
                                variables
                        );
                        notification.setHtmlContent(renderedHtml);
                    } catch (Exception e) {
                        log.debug("No HTML template available for: {}", notification.getTemplateCode());
                    }
                    
                    log.debug("Template rendered successfully: {}", notification.getTemplateCode());
                } else {
                    log.warn("Template not found: {}", notification.getTemplateCode());
                }
            } catch (Exception e) {
                log.error("Failed to render template: {}", notification.getTemplateCode(), e);
                // Continue with original message if template rendering fails
            }
        }
    }
    
    /**
     * Route notification to appropriate provider based on channel, country, and health
     * Requirement 2.2: Select provider based on channel, country, priority, and health
     */
    private NotificationProvider routeToProvider(Notification notification) {
        return providerRouter.selectProvider(
                        notification.getType(),
                        notification.getCountryCode(),
                        notification.getPriority()
                )
                .orElseThrow(() -> new BusinessException(
                        String.format("No healthy provider found for channel: %s, country: %s",
                                notification.getType(), notification.getCountryCode())
                ));
    }
    
    private void updateNotificationStatus(Notification notification, boolean success, String providerId) {
        notification.setProviderId(providerId);
        
        if (success) {
            notification.markAsSent();
            notification.setDeliveryStatus(Notification.DeliveryStatus.SENT);
            log.info("Notification sent successfully: eventId={}, provider={}", 
                    notification.getEventId(), providerId);
        } else {
            notification.markAsFailed("Provider failed to send notification");
            notification.setDeliveryStatus(Notification.DeliveryStatus.FAILED);
            log.error("Failed to send notification: eventId={}, provider={}", 
                    notification.getEventId(), providerId);
            
            // Schedule retry with exponential backoff and failover
            // Requirement 9.1, 9.2, 9.5: Retry with exponential backoff and failover
            if (retrySchedulerService.shouldRetry(notification)) {
                retrySchedulerService.scheduleRetry(notification, true);
            } else {
                retrySchedulerService.markAsPermanentlyFailed(notification, "Max retries exceeded");
            }
        }
    }
    
    /**
     * Extract user ID from notification metadata or use recipient as fallback
     */
    private String extractUserId(Notification notification) {
        if (notification.getMetadata() != null && notification.getMetadata().containsKey("userId")) {
            return notification.getMetadata().get("userId");
        }
        // Fallback to recipient as user ID
        return notification.getRecipient();
    }
}
