package com.immortals.notification.service.application.usecase.impl;


import com.immortals.notification.service.application.usecase.port.NotificationProvider;
import com.immortals.notification.service.application.usecase.port.NotificationRepository;
import com.immortals.notification.service.service.*;
import com.immortals.notification.service.service.impl.CountryCodeExtractor;
import com.immortals.notification.service.service.impl.NotificationMetricsService;
import com.immortals.notification.service.service.impl.ProviderRouter;
import com.immortals.platform.common.exception.BusinessException;
import com.immortals.platform.common.exception.ResourceNotFoundException;
import com.immortals.platform.domain.notifications.domain.Notification;
import com.immortals.platform.domain.notifications.domain.NotificationPriority;
import com.immortals.platform.domain.notifications.domain.UserPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final NotificationMetricsService metricsService;

    @Transactional
    public Notification execute(Notification notification) {
        log.info("Processing notification: eventId={}, type={}, recipient={}",
                notification.getEventId(), notification.getType(), notification.getRecipient());

        if (notificationRepository.existsByEventId(notification.getEventId())) {
            log.info("Notification already processed (repository check): eventId={}", notification.getEventId());
            return notificationRepository.findByEventId(notification.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", notification.getEventId()));
        }

        if (deduplicationService.isDuplicate(notification)) {
            log.info("Notification is duplicate (cache check): eventId={}", notification.getEventId());
            metricsService.recordDeduplicationHit(notification.getEventId());
            return notificationRepository.findByEventId(notification.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", notification.getEventId()));
        }

        initializeNotification(notification);

        extractCountryCode(notification);

        if (!checkUserPreferences(notification)) {
            notification.markAsFailed("User preferences do not allow this notification");
            return notificationRepository.save(notification);
        }

        if (!checkRateLimit(notification)) {
            notification.markAsFailed("Rate limit exceeded");
            return notificationRepository.save(notification);
        }

        renderTemplate(notification);

        var savedNotification = notificationRepository.save(notification);

        deduplicationService.markAsSent(savedNotification);

        var provider = routeToProvider(savedNotification);

        var success = provider.send(savedNotification);

        updateNotificationStatus(savedNotification, success, provider.getProviderId());

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
        if (notification.getCountryCode() == null || notification.getCountryCode()
                .isBlank()) {
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
        String userId = extractUserId(notification);

        if (!userPreferenceService.isChannelEnabled(userId, notification.getType())) {
            log.info("Notification blocked - channel disabled: userId={}, channel={}",
                    userId, notification.getType());
            return false;
        }

        if (isInQuietHours(userId)) {
            LocalDateTime delayUntil = calculateQuietHoursEnd(userId);
            if (delayUntil != null) {
                notification.setScheduledAt(delayUntil);
                notification.setStatus(Notification.NotificationStatus.SCHEDULED);
                log.info("Notification delayed due to quiet hours: userId={}, delayUntil={}",
                        userId, delayUntil);
                notificationRepository.save(notification);
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    /**
     * Check if current time is within user's quiet hours
     */
    private boolean isInQuietHours(String userId) {
        try {
            var preferenceOpt = userPreferenceService.getUserPreferences(userId);
            if (preferenceOpt.isEmpty()) {
                return Boolean.FALSE;
            }

            var preference = preferenceOpt.get();
            if (preference.getQuietHours() == null || !preference.getQuietHours()
                    .isEnabled()) {
                return Boolean.FALSE;
            }

            var quietHours = preference.getQuietHours();
            if (quietHours.getStartTime() == null || quietHours.getEndTime() == null) {
                return Boolean.FALSE;
            }

            return quietHours(quietHours);
        } catch (Exception e) {
            log.warn("Error checking quiet hours for user: {}", userId, e);
            return false;
        }
    }

    public static boolean quietHours(UserPreference.QuietHours quietHours) {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(quietHours.getStartTime());
        LocalTime end = LocalTime.parse(quietHours.getEndTime());

        if (start.isBefore(end)) {
            return now.isAfter(start) && now.isBefore(end);
        } else {
            return now.isAfter(start) || now.isBefore(end);
        }
    }

    /**
     * Calculate when quiet hours end to schedule notification
     */
    private java.time.LocalDateTime calculateQuietHoursEnd(String userId) {
        try {
            var preferenceOpt = userPreferenceService.getUserPreferences(userId);
            if (preferenceOpt.isEmpty()) {
                return null;
            }

            var preference = preferenceOpt.get();
            if (preference.getQuietHours() == null || !preference.getQuietHours()
                    .isEnabled()) {
                return null;
            }

            var quietHours = preference.getQuietHours();
            if (quietHours.getEndTime() == null) {
                return null;
            }

            LocalTime endTime = LocalTime.parse(quietHours.getEndTime());
            LocalTime now = LocalTime.now();

            LocalDateTime scheduledTime;
            if (endTime.isAfter(now)) {
                // Quiet hours end today
                scheduledTime = LocalDateTime.of(LocalDateTime.now()
                        .toLocalDate(), endTime);
            } else {
                // Quiet hours end tomorrow
                scheduledTime = LocalDateTime.of(LocalDateTime.now()
                        .toLocalDate()
                        .plusDays(1), endTime);
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
            metricsService.recordRateLimitExceeded(userId, notification.getType());
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
        if (notification.getTemplateCode() != null && !notification.getTemplateCode()
                .isBlank()) {
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
            notification.setProcessedAt(LocalDateTime.now());
            log.info("Notification sent successfully: eventId={}, provider={}",
                    notification.getEventId(), providerId);

            metricsService.recordNotificationSent(notification);
            metricsService.recordDeliveryTime(notification);
        } else {
            notification.markAsFailed("Provider failed to send notification");
            notification.setDeliveryStatus(Notification.DeliveryStatus.FAILED);
            log.error("Failed to send notification: eventId={}, provider={}",
                    notification.getEventId(), providerId);

            metricsService.recordNotificationFailure(notification, "Provider failed to send notification");

            if (retrySchedulerService.shouldRetry(notification)) {
                retrySchedulerService.scheduleRetry(notification, true);
                metricsService.recordRetry(notification);
            } else {
                retrySchedulerService.markAsPermanentlyFailed(notification, "Max retries exceeded");
            }
        }
    }

    /**
     * Extract user ID from notification metadata or use recipient as fallback
     */
    private String extractUserId(Notification notification) {
        if (notification.getMetadata() != null && notification.getMetadata()
                .containsKey("userId")) {
            return notification.getMetadata()
                    .get("userId");
        }
        return notification.getRecipient();
    }
}
