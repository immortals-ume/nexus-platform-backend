package com.immortals.notification.service.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "notification.service")
@Data
public class NotificationServiceProperties {

    private EventProperties events = new EventProperties();
    private ProviderProperties providers = new ProviderProperties();
    private MetricsProperties metrics = new MetricsProperties();
    private CacheProperties cache = new CacheProperties();
    private RetryProperties retry = new RetryProperties();
    private HealthCheckProperties healthCheck = new HealthCheckProperties();

    @Data
    public static class EventProperties {
        private String notificationRequestType = "NotificationRequest";
        private String notificationSentType = "NotificationSent";
        private String notificationSentTopic = "notification-sent";
        private String metricsListenerName = "NotificationEventListener";
    }

    @Data
    public static class ProviderProperties {
        private String smsProviderId = "SMS_TWILIO";
        private String failoverStrategyName = "FAILOVER";
        private Map<String, ProviderConfig> configs = new HashMap<>();
    }

    @Data
    public static class ProviderConfig {
        private String name;
        private boolean enabled = true;
        private int priority = 100;
    }

    @Data
    public static class MetricsProperties {
        private String sentCounterName = "notification.sent.total";
        private String deliveryTimerName = "notification.delivery.time";
        private String retryCounterName = "notification.retry.count";
        private String providerHealthGaugeName = "provider.health.status";
        private String templateRenderTimerName = "template.render.time";
        private String rateLimitCounterName = "rate.limit.exceeded";
        private String failedCounterName = "notification.failed.total";
        private String deduplicationCounterName = "notification.deduplication.hit";
        private String scheduledCounterName = "notification.scheduled.processed";
        private String webhookCounterName = "notification.webhook.received";
    }

    @Data
    public static class CacheProperties {
        private String providerHealthPrefix = "provider:health";
        private String deduplicationPrefix = "notification:dedup";
        private String rateLimitPrefix = "notification:ratelimit";
        private String templatePrefix = "notification:template";
        private String preferencePrefix = "notification:preference";
    }

    @Data
    public static class RetryProperties {
        private int maxAttempts = 3;
        private long initialIntervalMs = 1000;
        private double multiplier = 2.0;
        private long maxIntervalMs = 10000;
    }

    @Data
    public static class HealthCheckProperties {
        private int databaseTimeoutSeconds = 2;
        private String redisHealthCommand = "PONG";
        private String healthyStatus = "UP";
        private String unhealthyStatus = "DOWN";
        private String healthyMessage = "Provider is healthy";
        private String unhealthyMessage = "Provider is unhealthy";
    }
}
