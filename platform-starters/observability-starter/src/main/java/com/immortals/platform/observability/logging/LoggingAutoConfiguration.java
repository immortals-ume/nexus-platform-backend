package com.immortals.platform.observability.logging;

import com.immortals.platform.observability.config.ObservabilityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for structured logging with JSON format.
 * Configures Logback for JSON structured logging with correlation IDs, trace IDs, and span IDs.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class LoggingAutoConfiguration {

    /**
     * Register correlation ID filter
     */
    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        log.info("Registering correlation ID filter for request tracking");
        return new CorrelationIdFilter();
    }

    /**
     * Register request/response logging interceptor
     */
    @Bean
    @ConditionalOnProperty(prefix = "platform.observability.logging", name = "request-response-logging", havingValue = "true", matchIfMissing = false)
    public RequestResponseLoggingInterceptor requestResponseLoggingInterceptor() {
        log.info("Registering request/response logging interceptor");
        return new RequestResponseLoggingInterceptor();
    }
}
