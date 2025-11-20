package com.example.gateway.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability configuration for distributed tracing and metrics
 * Integrates with the observability-starter for consistent observability across services
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Enable @Observed annotation support for custom observations
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
