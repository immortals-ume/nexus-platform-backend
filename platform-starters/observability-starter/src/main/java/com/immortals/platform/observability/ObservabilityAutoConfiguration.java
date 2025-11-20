package com.immortals.platform.observability;

import com.immortals.platform.observability.config.ObservabilityProperties;
import com.immortals.platform.observability.logging.LoggingAutoConfiguration;
import com.immortals.platform.observability.metrics.MetricsAutoConfiguration;
import com.immortals.platform.observability.tracing.TracingAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Main auto-configuration class for platform observability features.
 * Imports all observability-related configurations including tracing, metrics, and logging.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
@Import({
    TracingAutoConfiguration.class,
    MetricsAutoConfiguration.class,
    LoggingAutoConfiguration.class
})
public class ObservabilityAutoConfiguration {

    public ObservabilityAutoConfiguration(ObservabilityProperties properties) {
        log.info("Initializing Platform Observability Starter");
        log.info("Tracing enabled: {}", properties.getTracing().isEnabled());
        log.info("Metrics enabled: {}", properties.getMetrics().isEnabled());
        log.info("Logging format: {}", properties.getLogging().getFormat());
    }
}
