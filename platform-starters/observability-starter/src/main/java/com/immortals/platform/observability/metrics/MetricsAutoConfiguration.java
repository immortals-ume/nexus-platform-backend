package com.immortals.platform.observability.metrics;

import com.immortals.platform.observability.config.ObservabilityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for metrics collection using Micrometer with Prometheus registry.
 * Configures JVM and system metrics, and enables custom business metrics.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "platform.observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsAutoConfiguration {

    /**
     * Customize meter registry with common tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            log.info("Configuring Micrometer metrics with Prometheus registry");
        };
    }

    /**
     * Register JVM memory metrics
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        log.debug("Registering JVM memory metrics");
        return new JvmMemoryMetrics();
    }

    /**
     * Register JVM GC metrics
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        log.debug("Registering JVM GC metrics");
        return new JvmGcMetrics();
    }

    /**
     * Register JVM thread metrics
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        log.debug("Registering JVM thread metrics");
        return new JvmThreadMetrics();
    }

    /**
     * Register class loader metrics
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        log.debug("Registering class loader metrics");
        return new ClassLoaderMetrics();
    }

    /**
     * Register processor metrics
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        log.debug("Registering processor metrics");
        return new ProcessorMetrics();
    }

    /**
     * Register uptime metrics
     */
    @Bean
    public UptimeMetrics uptimeMetrics() {
        log.debug("Registering uptime metrics");
        return new UptimeMetrics();
    }

    /**
     * Provide custom metrics service for business operations
     */
    @Bean
    public CustomMetricsService customMetricsService(MeterRegistry meterRegistry) {
        log.info("Initializing custom metrics service");
        return new CustomMetricsService(meterRegistry);
    }
}
