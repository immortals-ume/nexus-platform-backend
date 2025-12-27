package com.immortals.notification.service.service.impl;

import com.immortals.platform.domain.notifications.domain.model.Notification;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for recording notification metrics using Micrometer
 * Provides comprehensive observability for notification operations
 */
@Service
@Slf4j
public class NotificationMetricsService {

    private final MeterRegistry meterRegistry;

    public NotificationMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record notification sent metric
     * Counter: notification.sent.total
     * Tags: channel, provider, status
     */
    public void recordNotificationSent(Notification notification) {
        Counter.builder("notification.sent.total")
                .description("Total number of notifications sent")
                .tag("channel", notification.getType().name())
                .tag("provider", notification.getProviderId() != null ? notification.getProviderId() : "unknown")
                .tag("status", notification.getStatus().name())
                .tag("priority", notification.getPriority() != null ? notification.getPriority().name() : "NORMAL")
                .register(meterRegistry)
                .increment();

        log.debug("Recorded notification sent metric: channel={}, provider={}, status={}",
                notification.getType(), notification.getProviderId(), notification.getStatus());
    }

    /**
     * Record notification delivery time
     * Timer: notification.delivery.time
     * Tags: channel, provider
     */
    public void recordDeliveryTime(Notification notification) {
        if (notification.getCreatedAt() != null && notification.getProcessedAt() != null) {
            Duration duration = Duration.between(notification.getCreatedAt(), notification.getProcessedAt());

            Timer.builder("notification.delivery.time")
                    .description("Time taken to deliver notification")
                    .tag("channel", notification.getType().name())
                    .tag("provider", notification.getProviderId() != null ? notification.getProviderId() : "unknown")
                    .register(meterRegistry)
                    .record(duration.toMillis(), TimeUnit.MILLISECONDS);

            log.debug("Recorded delivery time metric: channel={}, provider={}, duration={}ms",
                    notification.getType(), notification.getProviderId(), duration.toMillis());
        }
    }

    /**
     * Record notification retry
     * Counter: notification.retry.count
     * Tags: channel, provider, retry_count
     */
    public void recordRetry(Notification notification) {
        Counter.builder("notification.retry.count")
                .description("Number of notification retries")
                .tag("channel", notification.getType().name())
                .tag("provider", notification.getProviderId() != null ? notification.getProviderId() : "unknown")
                .tag("retry_count", String.valueOf(notification.getRetryCount()))
                .register(meterRegistry)
                .increment();

        log.debug("Recorded retry metric: channel={}, provider={}, retryCount={}",
                notification.getType(), notification.getProviderId(), notification.getRetryCount());
    }

    /**
     * Record rate limit exceeded
     * Counter: rate.limit.exceeded
     * Tags: channel, user_id
     */
    public void recordRateLimitExceeded(String userId, Notification.NotificationType channel) {
        Counter.builder("rate.limit.exceeded")
                .description("Number of times rate limit was exceeded")
                .tag("channel", channel.name())
                .tag("user_type", userId.contains("@") ? "email" : "phone")
                .register(meterRegistry)
                .increment();

        log.debug("Recorded rate limit exceeded metric: channel={}, userId={}", channel, maskSensitiveData(userId));
    }

    /**
     * Record template rendering time
     * Timer: template.render.time
     * Tags: template_code, engine
     */
    public void recordTemplateRenderTime(String templateCode, String engine, long durationMillis) {
        Timer.builder("template.render.time")
                .description("Time taken to render template")
                .tag("template_code", templateCode)
                .tag("engine", engine)
                .register(meterRegistry)
                .record(durationMillis, TimeUnit.MILLISECONDS);

        log.debug("Recorded template render time metric: templateCode={}, engine={}, duration={}ms",
                templateCode, engine, durationMillis);
    }

    /**
     * Record provider health status
     * Gauge: provider.health.status
     * Tags: provider, channel
     * Value: 1 for healthy, 0 for unhealthy
     */
    public void recordProviderHealth(String providerId, Notification.NotificationType channel, boolean isHealthy) {
        meterRegistry.gauge("provider.health.status",
                java.util.Arrays.asList(
                        io.micrometer.core.instrument.Tag.of("provider", providerId),
                        io.micrometer.core.instrument.Tag.of("channel", channel.name())
                ),
                isHealthy ? 1.0 : 0.0);

        log.debug("Recorded provider health metric: provider={}, channel={}, healthy={}",
                providerId, channel, isHealthy);
    }

    /**
     * Record notification failure
     * Counter: notification.failed.total
     * Tags: channel, provider, failure_reason
     */
    public void recordNotificationFailure(Notification notification, String failureReason) {
        Counter.builder("notification.failed.total")
                .description("Total number of failed notifications")
                .tag("channel", notification.getType().name())
                .tag("provider", notification.getProviderId() != null ? notification.getProviderId() : "unknown")
                .tag("failure_reason", categorizeFailureReason(failureReason))
                .register(meterRegistry)
                .increment();

        log.debug("Recorded notification failure metric: channel={}, provider={}, reason={}",
                notification.getType(), notification.getProviderId(), failureReason);
    }

    /**
     * Record deduplication hit
     * Counter: notification.deduplication.hit
     */
    public void recordDeduplicationHit(String eventId) {
        Counter.builder("notification.deduplication.hit")
                .description("Number of duplicate notifications detected")
                .register(meterRegistry)
                .increment();

        log.debug("Recorded deduplication hit metric: eventId={}", eventId);
    }

    /**
     * Record scheduled notification processed
     * Counter: notification.scheduled.processed
     */
    public void recordScheduledNotificationProcessed() {
        Counter.builder("notification.scheduled.processed")
                .description("Number of scheduled notifications processed")
                .register(meterRegistry)
                .increment();

        log.debug("Recorded scheduled notification processed metric");
    }

    /**
     * Record webhook received
     * Counter: notification.webhook.received
     * Tags: provider, status
     */
    public void recordWebhookReceived(String provider, String status) {
        Counter.builder("notification.webhook.received")
                .description("Number of webhooks received from providers")
                .tag("provider", provider)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        log.debug("Recorded webhook received metric: provider={}, status={}", provider, status);
    }

    /**
     * Categorize failure reason for better metrics grouping
     */
    private String categorizeFailureReason(String failureReason) {
        if (failureReason == null) {
            return "unknown";
        }

        String reason = failureReason.toLowerCase();
        if (reason.contains("rate limit") || reason.contains("throttle")) {
            return "rate_limit";
        } else if (reason.contains("invalid") || reason.contains("malformed")) {
            return "invalid_input";
        } else if (reason.contains("timeout") || reason.contains("connection")) {
            return "network_error";
        } else if (reason.contains("authentication") || reason.contains("unauthorized")) {
            return "auth_error";
        } else if (reason.contains("provider")) {
            return "provider_error";
        } else if (reason.contains("preference") || reason.contains("disabled")) {
            return "user_preference";
        } else {
            return "other";
        }
    }

    /**
     * Mask sensitive data for logging
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() < 4) {
            return "***";
        }
        return data.substring(0, 2) + "***" + data.substring(data.length() - 2);
    }
}
