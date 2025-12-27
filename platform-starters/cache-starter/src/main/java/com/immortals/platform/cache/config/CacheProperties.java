package com.immortals.platform.cache.config;

import com.immortals.platform.cache.providers.caffeine.CaffeineProperties;
import com.immortals.platform.cache.providers.multilevel.MultiLevelCacheProperties;
import com.immortals.platform.cache.providers.redis.RedisProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the cache starter.
 *
 * <p>This class binds configuration properties under the "immortals.cache" prefix
 * and provides configuration for all cache providers and features.
 *
 * <p>Example configuration:
 * <pre>
 * immortals:
 *   cache:
 *     enabled: true
 *     type: multi-level
 *     default-ttl: 1h
 *     caffeine:
 *       maximum-size: 10000
 *     redis:
 *       host: localhost
 *       port: 6379
 * </pre>
 *
 * @since 2.0.0
 */
@Data
@ConfigurationProperties(prefix = "immortals.cache")
public class CacheProperties {

    /**
     * Whether caching is enabled.
     */
    private Boolean enabled = true;

    /**
     * Cache provider type (caffeine, redis, multi-level).
     */
    private String type = "caffeine";

    /**
     * Default TTL for cache entries.
     */
    private Duration defaultTtl = Duration.ofHours(1);

    /**
     * Namespace-specific configurations.
     */
    private Map<String, NamespaceConfig> namespaces = new HashMap<>();

    /**
     * Caffeine cache configuration.
     */
    @NestedConfigurationProperty
    private CaffeineProperties caffeine = new CaffeineProperties();

    /**
     * Redis cache configuration.
     */
    @NestedConfigurationProperty
    private RedisProperties redisProperties = new RedisProperties();

    /**
     * Multi-level cache configuration.
     */
    @NestedConfigurationProperty
    private MultiLevelCacheProperties multilevel = new MultiLevelCacheProperties();

    /**
     * Resilience configuration.
     */
    @NestedConfigurationProperty
    private ResilienceProperties resilience = new ResilienceProperties();

    /**
     * Feature configuration.
     */
    @NestedConfigurationProperty
    private FeatureProperties features = new FeatureProperties();

    /**
     * Observability configuration.
     */
    @NestedConfigurationProperty
    private ObservabilityProperties observability = new ObservabilityProperties();

    /**
     * Namespace-specific configuration.
     */
    @Data
    public static class NamespaceConfig {
        private Duration ttl;
        private String provider;
        private Map<String, Object> properties = new HashMap<>();
    }

    /**
     * Resilience configuration.
     */
    @Data
    public static class ResilienceProperties {
        @NestedConfigurationProperty
        private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

        @NestedConfigurationProperty
        private StampedeProtectionProperties stampedeProtection = new StampedeProtectionProperties();

        @NestedConfigurationProperty
        private TimeoutProperties timeout = new TimeoutProperties();
    }

    /**
     * Circuit breaker configuration.
     */
    @Data
    public static class CircuitBreakerProperties {
        private boolean enabled = false;
        private int failureRateThreshold = 50;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int slidingWindowSize = 10;
    }

    /**
     * Stampede protection configuration.
     */
    @Data
    public static class StampedeProtectionProperties {
        private boolean enabled = false;
        private Duration lockTimeout = Duration.ofSeconds(5);
    }

    /**
     * Timeout configuration.
     */
    @Data
    public static class TimeoutProperties {
        private boolean enabled = false;
        private Duration duration = Duration.ofSeconds(2);
    }

    /**
     * Feature configuration.
     */
    @Data
    public static class FeatureProperties {
        @NestedConfigurationProperty
        private CompressionProperties compression = new CompressionProperties();

        @NestedConfigurationProperty
        private EncryptionProperties encryption = new EncryptionProperties();

        @NestedConfigurationProperty
        private DistributedEvictionProperties distributedEviction = new DistributedEvictionProperties();
    }

    /**
     * Compression configuration.
     */
    @Data
    public static class CompressionProperties {
        private boolean enabled = false;
        private int threshold = 1024;
        private String algorithm = "gzip";
    }

    /**
     * Encryption configuration.
     */
    @Data
    public static class EncryptionProperties {
        private boolean enabled = false;
        private String algorithm = "AES";
        private String key;
    }

    /**
     * Distributed eviction configuration.
     */
    @Data
    public static class DistributedEvictionProperties {
        private boolean enabled = false;
        private String channel = "cache-eviction";
    }

    /**
     * Observability configuration.
     */
    @Data
    public static class ObservabilityProperties {
        @NestedConfigurationProperty
        private MetricsProperties metrics = new MetricsProperties();

        @NestedConfigurationProperty
        private HealthCheckProperties healthChecks = new HealthCheckProperties();

        @NestedConfigurationProperty
        private TracingProperties tracing = new TracingProperties();
    }

    /**
     * Metrics configuration.
     */
    @Data
    public static class MetricsProperties {
        private boolean enabled = true;
        private boolean detailed = false;
    }

    /**
     * Health check configuration.
     */
    @Data
    public static class HealthCheckProperties {
        private boolean enabled = true;
    }

    /**
     * Tracing configuration.
     */
    @Data
    public static class TracingProperties {
        private boolean enabled = false;
    }
}