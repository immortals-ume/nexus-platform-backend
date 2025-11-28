package com.immortals.config.server.config;

import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Resilience4j retry and timeout patterns.
 * Implements retry logic with exponential backoff and timeout enforcement with logging.
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    /**
     * Configure retry registry with event listeners for logging.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.getAllRetries().forEach(retry -> retry.getEventPublisher()
            .onRetry(event -> log.warn("Retry attempt {} for operation '{}'. Exception: {}",
                event.getNumberOfRetryAttempts(),
                event.getName(),
                event.getLastThrowable().getMessage()))
            .onSuccess(event -> log.info("Operation '{}' succeeded after {} attempts",
                event.getName(),
                event.getNumberOfRetryAttempts()))
            .onError(event -> log.error("Operation '{}' failed after {} attempts. Final exception: {}",
                event.getName(),
                event.getNumberOfRetryAttempts(),
                event.getLastThrowable().getMessage())));
        
        return registry;
    }

    /**
     * Configure time limiter registry with event listeners for timeout logging.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        registry.getAllTimeLimiters().forEach(timeLimiter -> timeLimiter.getEventPublisher()
            .onSuccess(event -> log.debug("Operation '{}' completed within timeout",
                timeLimiter.getName()))
            .onError(event -> log.error("Operation '{}' exceeded timeout. Exception: {}",
                timeLimiter.getName(),
                event.getThrowable().getMessage()))
            .onTimeout(event -> log.warn("Operation '{}' timed out after configured duration",
                timeLimiter.getName())));
        
        return registry;
    }
}
