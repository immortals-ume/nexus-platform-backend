package com.immortals.notificationservice.configuration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate limiting configuration to prevent overwhelming external services
 */
@Configuration
public class RateLimitConfiguration {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public RateLimiter emailRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100) // 100 requests
                .limitRefreshPeriod(Duration.ofSeconds(1)) // per second
                .timeoutDuration(Duration.ofSeconds(5)) // wait up to 5 seconds
                .build();
        
        return registry.rateLimiter("emailService", config);
    }

    @Bean
    public RateLimiter smsRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50) // 50 requests (Twilio limits)
                .limitRefreshPeriod(Duration.ofSeconds(1)) // per second
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        return registry.rateLimiter("smsService", config);
    }
}
