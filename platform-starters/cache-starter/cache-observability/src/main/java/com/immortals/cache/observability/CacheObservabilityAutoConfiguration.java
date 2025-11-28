package com.immortals.cache.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for cache observability features.
 * Enables AOP-based metrics, tracing, logging, and health checks.
 * 
 * @since 2.0.0
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@ConditionalOnProperty(
    prefix = "immortals.cache.observability",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CacheObservabilityAutoConfiguration {

    /**
     * Creates the structured logger bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheStructuredLogger cacheStructuredLogger() {
        return new CacheStructuredLogger();
    }

    /**
     * Creates the tracing service bean if OpenTelemetry is available.
     */
    @Bean
    @ConditionalOnClass(OpenTelemetry.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "immortals.cache.observability.tracing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public CacheTracingService cacheTracingService(OpenTelemetry openTelemetry) {
        return new CacheTracingService(openTelemetry);
    }

    /**
     * Creates the observability aspect bean.
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "immortals.cache.observability.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public CacheObservabilityAspect cacheObservabilityAspect(
            MeterRegistry meterRegistry,
            CacheStructuredLogger structuredLogger) {
        return new CacheObservabilityAspect(meterRegistry, java.util.Optional.empty(), structuredLogger);
    }

    /**
     * Creates the health indicator bean.
     */
    @Bean
    @ConditionalOnEnabledHealthIndicator("cache")
    @ConditionalOnMissingBean
    public CacheHealthIndicator cacheHealthIndicator() {
        return new CacheHealthIndicator(java.util.Collections.emptyList(), java.util.Optional.empty());
    }
}
