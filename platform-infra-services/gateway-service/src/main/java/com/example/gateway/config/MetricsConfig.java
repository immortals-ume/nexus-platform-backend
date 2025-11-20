package com.example.gateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom metrics configuration for gateway operations
 * Provides additional metrics beyond standard Spring Cloud Gateway metrics
 */
@Configuration
public class MetricsConfig {

    /**
     * Counter for total requests processed by the gateway
     */
    @Bean
    public Counter gatewayRequestCounter(MeterRegistry registry) {
        return Counter.builder("gateway.requests.total")
            .description("Total number of requests processed by the gateway")
            .register(registry);
    }

    /**
     * Counter for circuit breaker activations
     */
    @Bean
    public Counter circuitBreakerActivationCounter(MeterRegistry registry) {
        return Counter.builder("gateway.circuitbreaker.activations")
            .description("Number of times circuit breakers have been activated")
            .register(registry);
    }

    /**
     * Counter for rate limit violations
     */
    @Bean
    public Counter rateLimitViolationCounter(MeterRegistry registry) {
        return Counter.builder("gateway.ratelimit.violations")
            .description("Number of requests rejected due to rate limiting")
            .register(registry);
    }

    /**
     * Counter for fallback invocations
     */
    @Bean
    public Counter fallbackInvocationCounter(MeterRegistry registry) {
        return Counter.builder("gateway.fallback.invocations")
            .description("Number of times fallback endpoints were invoked")
            .register(registry);
    }
}
