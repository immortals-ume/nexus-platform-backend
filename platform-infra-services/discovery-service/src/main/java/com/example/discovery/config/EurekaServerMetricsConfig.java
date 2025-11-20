package com.example.discovery.config;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for Eureka Server with custom metrics and health checks
 */
@Slf4j
@Configuration
public class EurekaServerMetricsConfig {

    /**
     * Custom health indicator for Eureka Server
     */
    @Bean
    public HealthIndicator eurekaServerHealthIndicator(EurekaServerContext eurekaServerContext) {
        return () -> {
            try {
                PeerAwareInstanceRegistry registry = eurekaServerContext.getRegistry();
                List<Application> applications = registry.getSortedApplications();
                
                int registeredInstances = applications.stream()
                    .mapToInt(app -> app.getInstances().size())
                    .sum();
                
                int upInstances = applications.stream()
                    .flatMap(app -> app.getInstances().stream())
                    .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)
                    .mapToInt(instance -> 1)
                    .sum();
                
                log.debug("Eureka health check - registered instances: {}, up instances: {}", 
                    registeredInstances, upInstances);
                
                return Health.up()
                    .withDetail("registeredInstances", registeredInstances)
                    .withDetail("upInstances", upInstances)
                    .withDetail("applications", applications.size())
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
     */
    @Bean
    public MeterBinder eurekaServerMetrics(EurekaServerContext eurekaServerContext) {
        return (MeterRegistry registry) -> {
            // Gauge for total registered instances
            registry.gauge("eureka.registered.instances", eurekaServerContext, context -> {
                try {
                    PeerAwareInstanceRegistry instanceRegistry = context.getRegistry();
                    return instanceRegistry.getSortedApplications().stream()
                        .mapToInt(app -> app.getInstances().size())
                        .sum();
                } catch (Exception e) {
                    log.error("Error collecting registered instances metric", e);
                    return 0;
                }
            });

            // Gauge for UP instances
            registry.gauge("eureka.up.instances", eurekaServerContext, context -> {
                try {
                    PeerAwareInstanceRegistry instanceRegistry = context.getRegistry();
                    return instanceRegistry.getSortedApplications().stream()
                        .flatMap(app -> app.getInstances().stream())
                        .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)
                        .count();
                } catch (Exception e) {
                    log.error("Error collecting UP instances metric", e);
                    return 0;
                }
            });

            // Gauge for total registered applications
            registry.gauge("eureka.registered.applications", eurekaServerContext, context -> {
                try {
                    PeerAwareInstanceRegistry instanceRegistry = context.getRegistry();
                    return instanceRegistry.getSortedApplications().size();
                } catch (Exception e) {
                    log.error("Error collecting registered applications metric", e);
                    return 0;
                }
            });

            // Gauge for available replicas
            registry.gauge("eureka.available.replicas", eurekaServerContext, context -> {
                try {
                    return context.getPeerEurekaNodes().getPeerEurekaNodes().size();
                } catch (Exception e) {
                    log.error("Error collecting available replicas metric", e);
                    return 0;
                }
            });

            log.info("Eureka Server custom metrics registered");
        };
    }
}
