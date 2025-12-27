package com.immortals.platform.cache.observability;

import com.immortals.platform.cache.providers.caffeine.L1CacheService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cache observability features.
 * Tests that metrics, health checks, and tracing work correctly.
 */
class ObservabilityIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    com.immortals.platform.cache.config.CacheAutoConfiguration.class,
                    com.immortals.platform.cache.config.CaffeineAutoConfiguration.class,
                    CacheObservabilityAutoConfiguration.class
            ))
            .withUserConfiguration(TestObservabilityConfiguration.class);

    @Test
    void shouldLoadHealthIndicatorWhenObservabilityEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.observability.enabled=true",
                        "platform.cache.observability.health.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheHealthIndicator.class);
                    
                    CacheHealthIndicator healthIndicator = context.getBean(CacheHealthIndicator.class);
                    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
                });
    }

    @Test
    void shouldLoadMetricsWhenObservabilityEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.observability.enabled=true",
                        "platform.cache.observability.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheMetrics.class);
                    assertThat(context).hasSingleBean(MeterRegistry.class);
                });
    }

    @Test
    void shouldNotLoadObservabilityWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.observability.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheHealthIndicator.class);
                    assertThat(context).doesNotHaveBean(CacheMetrics.class);
                });
    }

    @Test
    void shouldRecordMetricsForCacheOperations() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.observability.enabled=true",
                        "platform.cache.observability.metrics.enabled=true"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
                    
                    // Perform cache operations
                    cacheService.put("metrics-key", "metrics-value");
                    cacheService.get("metrics-key");
                    cacheService.get("non-existent-key");
                    
                    // Verify metrics are recorded (basic check)
                    assertThat(meterRegistry.getMeters()).isNotEmpty();
                });
    }

    @Test
    void shouldProvideHealthStatusBasedOnCacheState() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.observability.enabled=true",
                        "platform.cache.observability.health.enabled=true"
                )
                .run(context -> {
                    CacheHealthIndicator healthIndicator = context.getBean(CacheHealthIndicator.class);
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    // Initially should be UP
                    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
                    
                    // After cache operations, should still be UP
                    cacheService.put("health-key", "health-value");
                    assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
                });
    }

    @Test
    void shouldLoadTracingWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.observability.enabled=true",
                        "platform.cache.observability.tracing.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheTracingService.class);
                });
    }

    @Test
    void shouldLoadStructuredLoggingWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.observability.enabled=true",
                        "platform.cache.observability.logging.structured=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheStructuredLogger.class);
                });
    }

    @Configuration
    static class TestObservabilityConfiguration {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}