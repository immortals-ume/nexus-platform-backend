package com.immortals.authapp.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j patterns including circuit breaker, retry, timeout, and bulkhead.
 * Implements requirements 5.1, 5.2, 5.3, 5.4 for resilience patterns in auth-service.
 */
@Configuration
@Slf4j
public class ResilienceConfig {

    /**
     * Circuit Breaker configuration for external service calls (e.g., OTP service).
     * Requirement 5.1: Implement circuit breaker pattern using Resilience4j
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .slowCallRateThreshold(50) // Open circuit if 50% of calls are slow
                .slowCallDurationThreshold(Duration.ofSeconds(3)) // Call is slow if > 3 seconds
                .waitDurationInOpenState(Duration.ofSeconds(10)) // Wait 10s before half-open
                .permittedNumberOfCallsInHalfOpenState(5) // Allow 5 calls in half-open state
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10) // Use last 10 calls for failure rate
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class) // Record all exceptions
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        
        // Add event listeners for monitoring
        registry.circuitBreaker("otpService").getEventPublisher()
                .onStateTransition(event -> log.info("OTP Service Circuit Breaker state changed: {}", event))
                .onError(event -> log.error("OTP Service Circuit Breaker error: {}", event))
                .onSuccess(event -> log.debug("OTP Service Circuit Breaker success: {}", event));

        registry.circuitBreaker("databaseOperations").getEventPublisher()
                .onStateTransition(event -> log.info("Database Circuit Breaker state changed: {}", event))
                .onError(event -> log.error("Database Circuit Breaker error: {}", event));

        return registry;
    }

    /**
     * Retry configuration for transient failures.
     * Requirement 5.2: Add retry logic with exponential backoff for transient failures
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3) // Maximum 3 attempts
                .waitDuration(Duration.ofMillis(500)) // Initial wait 500ms
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(500, 2)) // Exponential backoff: 500ms, 1000ms, 2000ms
                .retryExceptions(
                        org.springframework.dao.TransientDataAccessException.class,
                        org.springframework.dao.QueryTimeoutException.class,
                        java.sql.SQLException.class,
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class
                )
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        
        // Add event listeners
        registry.retry("databaseOperations").getEventPublisher()
                .onRetry(event -> log.warn("Database operation retry attempt {}: {}", 
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.debug("Database operation succeeded after {} attempts", 
                        event.getNumberOfRetryAttempts()));

        registry.retry("otpService").getEventPublisher()
                .onRetry(event -> log.warn("OTP service retry attempt {}: {}", 
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));

        return registry;
    }

    /**
     * Time Limiter configuration for timeout management.
     * Requirement 5.3: Configure timeouts for all external calls (5 seconds max)
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5)) // 5 seconds timeout
                .cancelRunningFuture(true) // Cancel the running future on timeout
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);
        
        // Add event listeners
        registry.timeLimiter("otpService").getEventPublisher()
                .onTimeout(event -> log.error("OTP service call timed out after 5 seconds"))
                .onSuccess(event -> log.debug("OTP service call completed within timeout"));

        registry.timeLimiter("externalService").getEventPublisher()
                .onTimeout(event -> log.error("External service call timed out after 5 seconds"));

        return registry;
    }

    /**
     * Bulkhead configuration for thread pool isolation.
     * Requirement 5.4: Implement bulkhead for thread pool isolation
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(10) // Maximum 10 concurrent calls
                .maxWaitDuration(Duration.ofMillis(500)) // Wait max 500ms for permission
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);
        
        // Add event listeners
        registry.bulkhead("otpService").getEventPublisher()
                .onCallPermitted(event -> log.debug("OTP service call permitted"))
                .onCallRejected(event -> log.warn("OTP service call rejected - bulkhead full"))
                .onCallFinished(event -> log.debug("OTP service call finished"));

        registry.bulkhead("authenticationService").getEventPublisher()
                .onCallRejected(event -> log.warn("Authentication service call rejected - bulkhead full"));

        return registry;
    }
}
