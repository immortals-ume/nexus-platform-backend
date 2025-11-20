package com.immortals.platform.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for platform messaging.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "platform.messaging")
public class MessagingProperties {

    /**
     * Kafka broker configuration
     */
    private Kafka kafka = new Kafka();

    /**
     * Retry configuration
     */
    private Retry retry = new Retry();

    /**
     * Dead letter queue configuration
     */
    private DeadLetterQueue dlq = new DeadLetterQueue();

    /**
     * Idempotency configuration
     */
    private Idempotency idempotency = new Idempotency();

    @Data
    public static class Kafka {
        /**
         * Kafka bootstrap servers
         */
        @NotBlank
        private String bootstrapServers = "localhost:9092";

        /**
         * Consumer group ID prefix
         */
        @NotBlank
        private String consumerGroupPrefix = "platform";

        /**
         * Number of consumer threads per topic
         */
        @Min(1)
        private int concurrency = 3;

        /**
         * Enable auto-commit for consumers
         */
        private boolean autoCommit = false;

        /**
         * Auto-offset reset strategy
         */
        private String autoOffsetReset = "earliest";

        /**
         * Maximum poll records
         */
        @Min(1)
        private int maxPollRecords = 500;

        /**
         * Session timeout
         */
        private Duration sessionTimeout = Duration.ofSeconds(30);

        /**
         * Heartbeat interval
         */
        private Duration heartbeatInterval = Duration.ofSeconds(10);
    }

    @Data
    public static class Retry {
        /**
         * Enable retry mechanism
         */
        private boolean enabled = true;

        /**
         * Maximum number of retry attempts
         */
        @Min(0)
        private int maxAttempts = 3;

        /**
         * Initial backoff duration
         */
        @NotNull
        private Duration initialInterval = Duration.ofSeconds(1);

        /**
         * Maximum backoff duration
         */
        @NotNull
        private Duration maxInterval = Duration.ofSeconds(30);

        /**
         * Backoff multiplier for exponential backoff
         */
        @Min(1)
        private double multiplier = 2.0;
    }

    @Data
    public static class DeadLetterQueue {
        /**
         * Enable dead letter queue
         */
        private boolean enabled = true;

        /**
         * DLQ topic suffix
         */
        @NotBlank
        private String topicSuffix = ".dlq";

        /**
         * Enable manual retry from DLQ
         */
        private boolean enableManualRetry = true;

        /**
         * Maximum retention time for DLQ messages
         */
        private Duration retentionTime = Duration.ofDays(7);
    }

    @Data
    public static class Idempotency {
        /**
         * Enable idempotency checking
         */
        private boolean enabled = true;

        /**
         * Redis key prefix for idempotency tracking
         */
        @NotBlank
        private String keyPrefix = "messaging:idempotency:";

        /**
         * TTL for idempotency keys
         */
        @NotNull
        private Duration ttl = Duration.ofHours(24);
    }
}
