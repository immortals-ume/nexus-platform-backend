package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.features.core.FeatureProperties;
import com.immortals.cache.observability.ObservabilityProperties;
import com.immortals.cache.providers.caffeine.CaffeineProperties;
import com.immortals.cache.providers.multilevel.MultiLevelCacheProperties;
import com.immortals.cache.providers.redis.RedisProperties;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "immortals.cache")
public class CacheProperties {

    /**
     * Cache type: caffeine, redis, multi-level.
     * Default: caffeine
     */
    @NotNull
    private CacheType type;
    /**
     * Default TTL for cache entries.
     * Default: 1 hour
     */
    @NotNull
    private Duration defaultTtl;
    /**
     * Whether caching is enabled globally.
     * Default: true
     */
    private Boolean enabled;
    /**
     * Namespace-specific configurations.
     * Key: namespace name, Value: namespace configuration
     */
    private Map<String, NamespaceConfig> namespaces = new HashMap<>();
    /**
     * Caffeine-specific configuration.
     */
    @NestedConfigurationProperty
    private CaffeineProperties caffeine = new CaffeineProperties();
    /**
     * Redis-specific configuration.
     */
    @NestedConfigurationProperty
    private RedisProperties redisProperties = new RedisProperties();
    /**
     * Multi-level cache configuration.
     */
    @NestedConfigurationProperty
    private MultiLevelCacheProperties multiLevel = new MultiLevelCacheProperties();
    /**
     * Feature toggles (compression, encryption, serialization).
     */
    @NestedConfigurationProperty
    private FeatureProperties features = new FeatureProperties();
    /**
     * Resilience configuration (circuit breaker, stampede protection, timeouts).
     */
    @NestedConfigurationProperty
    private ResilienceProperties resilience = new ResilienceProperties();
    /**
     * Observability configuration (metrics, health checks, tracing, logging).
     */
    @NestedConfigurationProperty
    private ObservabilityProperties observability = new ObservabilityProperties();

    /**
     * Validates the cache properties after binding.
     * Called automatically by Spring after property binding.
     */
    @PostConstruct
    public void validate() {
        validateCacheType();
        validateDefaultTtl();
        validateEncryptionKey();
    }

    /**
     * Validates that cache type is one of the allowed values.
     */
    private void validateCacheType() {
        if (type != null) {
            boolean isValid = Stream.of(CacheType.values())
                    .anyMatch(ct -> ct == type);
            if (!isValid) {
                throw new IllegalArgumentException(
                        String.format("Invalid cache type: '%s'. Must be one of: %s",
                                type, String.join(", ",
                                        Stream.of(CacheType.values())
                                                .map(Enum::name)
                                                .toArray(String[]::new))));
            }
        }
    }

    /**
     * Validates that default TTL is a positive duration.
     */
    private void validateDefaultTtl() {
        if (defaultTtl != null && defaultTtl.isNegative()) {
            throw new IllegalArgumentException(
                    String.format("Invalid default TTL: '%s'. TTL must be a positive duration.",
                            defaultTtl));
        }
        if (defaultTtl != null && defaultTtl.isZero()) {
            throw new IllegalArgumentException(
                    "Invalid default TTL: duration cannot be zero. TTL must be a positive duration.");
        }
    }

    /**
     * Validates that encryption key is provided if encryption is enabled.
     */
    private void validateEncryptionKey() {
        if (features != null && features.getEncryption() != null) {
            if (features.getEncryption()
                    .isEnabled() &&
                    (features.getEncryption()
                            .getKey() == null ||
                            features.getEncryption()
                                    .getKey()
                                    .trim()
                                    .isEmpty())) {
                throw new IllegalArgumentException(
                        "Encryption is enabled but no encryption key is provided. " +
                                "Please set 'immortals.cache.features.encryption.key' property.");
            }
        }
    }

    /**
     * Cache type enumeration.
     */
    public enum CacheType {
        CAFFEINE,
        REDIS,
        MULTI_LEVEL
    }

    /**
     * Namespace-specific configuration.
     */
    @Data
    public static class NamespaceConfig {
        private Duration ttl;
        private Boolean compressionEnabled;
        private Boolean encryptionEnabled;
        private Boolean stampedeProtectionEnabled;
    }


    /**
     * Resilience configuration.
     */
    @Data
    public static class ResilienceProperties {
        /**
         * Circuit breaker configuration.
         */
        private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

        /**
         * Stampede protection configuration.
         */
        private StampedeProtectionProperties stampedeProtection = new StampedeProtectionProperties();

        /**
         * Timeout configuration.
         */
        private TimeoutProperties timeout = new TimeoutProperties();

        /**
         * Circuit breaker configuration.
         */
        @Data
        public static class CircuitBreakerProperties {
            /**
             * Whether circuit breaker is enabled.
             * Default: false
             */
            private boolean enabled = false;

            /**
             * Failure rate threshold percentage to open the circuit.
             * Default: 50
             */
            @Min(1)
            private int failureRateThreshold = 50;

            /**
             * Wait duration in open state before transitioning to half-open.
             * Default: 60 seconds
             */
            @NotNull
            private Duration waitDurationInOpenState = Duration.ofSeconds(60);

            /**
             * Sliding window size for calculating failure rate.
             * Default: 100
             */
            @Min(1)
            private int slidingWindowSize = 100;

            /**
             * Minimum number of calls before calculating failure rate.
             * Default: 10
             */
            @Min(1)
            private int minimumNumberOfCalls = 10;
        }

        /**
         * Stampede protection configuration.
         */
        @Data
        public static class StampedeProtectionProperties {
            /**
             * Whether stampede protection is enabled.
             * Default: false
             */
            private boolean enabled = false;

            /**
             * Timeout for acquiring distributed lock.
             * Default: 5 seconds
             */
            @NotNull
            private Duration lockTimeout = Duration.ofSeconds(5);
        }

        /**
         * Timeout configuration.
         */
        @Data
        public static class TimeoutProperties {
            /**
             * Whether timeout handling is enabled.
             * Default: true
             */
            private boolean enabled = true;

            /**
             * Operation timeout duration.
             * Default: 5 seconds
             */
            @NotNull
            private Duration operationTimeout = Duration.ofSeconds(5);
        }
    }


}
