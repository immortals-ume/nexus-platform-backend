package com.example.discovery.config;

import com.example.discovery.metrics.EurekaMetrics;
import com.example.discovery.metrics.RegistryStatistics;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for Eureka Server with custom metrics and health checks
 * Integrates with Micrometer/Prometheus for metrics exposure
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EurekaServerMetricsConfig {

    private final EurekaMetrics eurekaMetrics;

    /**
     * Custom health indicator for Eureka Server
     * Provides detailed health information including registry state
     */
    @Bean
    public HealthIndicator eurekaServerHealthIndicator(EurekaServerContext eurekaServerContext) {
        return () -> {
            try {
                RegistryStatistics stats = eurekaMetrics.getRegistryStatistics();
                
                log.debug("Eureka health check - registered instances: {}, up instances: {}",
                    stats.getTotalInstances(), stats.getUpInstances());
                
                return Health.up()
                    .withDetail("registeredInstances", stats.getTotalInstances())
                    .withDetail("upInstances", stats.getUpInstances())
                    .withDetail("downInstances", stats.getDownInstances())
                    .withDetail("applications", stats.getTotalApplications())
                    .withDetail("availableReplicas", stats.getAvailableReplicas())
                    .withDetail("selfPreservationMode", stats.isSelfPreservationMode())
                    .withDetail("renewsLastMinute", stats.getRenewsLastMinute())
                    .withDetail("status", "UP")
                    .build();
            } catch (Exception e) {
                log.error("Eureka health check failed", e);
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    /**
     * Custom metrics for Eureka Server
     * Exposes comprehensive registry metrics via Micrometer/Prometheus
     */
    @Bean
    public MeterBinder eurekaServerMetrics(EurekaServerContext eurekaServerContext) {
        return (MeterRegistry registry) -> {
            registry.gauge("eureka.registered.instances", eurekaMetrics, EurekaMetrics::getRegisteredInstancesCount);

            registry.gauge("eureka.up.instances", eurekaMetrics, EurekaMetrics::getUpInstancesCount);

            registry.gauge("eureka.down.instances", eurekaMetrics, EurekaMetrics::getDownInstancesCount);

            registry.gauge("eureka.registered.applications", eurekaMetrics, EurekaMetrics::getApplicationsCount);

            registry.gauge("eureka.available.replicas", eurekaMetrics, EurekaMetrics::getAvailableReplicasCount);

            registry.gauge("eureka.self.preservation.mode", eurekaMetrics,
                metrics -> metrics.isSelfPreservationModeEnabled() ? 1.0 : 0.0);

            registry.gauge("eureka.renews.last.minute", eurekaMetrics, EurekaMetrics::getRenewsLastMinute);

            log.info("Eureka Server custom metrics registered with Micrometer/Prometheus");
        };
    }
}
