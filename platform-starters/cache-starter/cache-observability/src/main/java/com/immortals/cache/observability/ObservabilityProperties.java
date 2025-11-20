package com.immortals.cache.observability;

import lombok.Data;

/**
 * Observability configuration.
 */
@Data
public class ObservabilityProperties {
    /**
     * Metrics configuration.
     */
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * Health check configuration.
     */
    private HealthProperties health = new HealthProperties();

    /**
     * Tracing configuration.
     */
    private TracingProperties tracing = new TracingProperties();

    /**
     * Logging configuration.
     */
    private LoggingProperties logging = new LoggingProperties();

    /**
     * Metrics configuration.
     */
    @Data
    public static class MetricsProperties {
        /**
         * Whether metrics collection is enabled.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Metrics prefix for all cache metrics.
         * Default: cache
         */
        private String prefix = "cache";
    }

    /**
     * Health check configuration.
     */
    @Data
    public static class HealthProperties {
        /**
         * Whether health indicator is enabled.
         * Default: true
         */
        private boolean enabled = true;
    }

    /**
     * Tracing configuration.
     */
    @Data
    public static class TracingProperties {
        /**
         * Whether distributed tracing is enabled.
         * Default: false
         */
        private boolean enabled = false;
    }

    /**
     * Logging configuration.
     */
    @Data
    public static class LoggingProperties {
        /**
         * Whether structured logging is enabled.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Whether to include correlation IDs in logs.
         * Default: true
         */
        private boolean includeCorrelationId = true;
    }
}