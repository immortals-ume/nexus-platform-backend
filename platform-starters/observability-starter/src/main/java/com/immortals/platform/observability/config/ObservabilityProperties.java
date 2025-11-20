package com.immortals.platform.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for platform observability features.
 * Allows external configuration of tracing, metrics, and logging.
 */
@Data
@ConfigurationProperties(prefix = "platform.observability")
public class ObservabilityProperties {

    /**
     * Tracing configuration
     */
    private Tracing tracing = new Tracing();

    /**
     * Metrics configuration
     */
    private Metrics metrics = new Metrics();

    /**
     * Logging configuration
     */
    private Logging logging = new Logging();

    @Data
    public static class Tracing {
        /**
         * Enable or disable distributed tracing
         */
        private boolean enabled = true;

        /**
         * Sampling rate for traces (0.0 to 1.0)
         * Default is 0.1 (10% sampling)
         */
        private double samplingRate = 0.1;

        /**
         * Exporter type (zipkin, jaeger, otlp)
         */
        private String exporter = "zipkin";

        /**
         * Zipkin endpoint URL
         */
        private String zipkinUrl = "http://localhost:9411";
    }

    @Data
    public static class Metrics {
        /**
         * Enable or disable metrics collection
         */
        private boolean enabled = true;

        /**
         * Metrics export interval in seconds
         */
        private int exportInterval = 60;
    }

    @Data
    public static class Logging {
        /**
         * Log format (json or text)
         */
        private String format = "json";

        /**
         * Default log level
         */
        private String level = "INFO";
    }
}
