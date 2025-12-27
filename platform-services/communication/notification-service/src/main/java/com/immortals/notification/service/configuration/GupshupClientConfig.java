package com.immortals.notification.service.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for Gupshup HTTP client
 * Provides RestTemplate configured for Gupshup API calls
 */
@Slf4j
@Configuration
public class GupshupClientConfig {
    
    private static final String GUPSHUP_BASE_URL = "https://api.gupshup.io";
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 30;
    
    /**
     * Create RestTemplate bean for Gupshup API calls
     * Configured with timeouts and logging interceptor
     */
    @Bean(name = "gupshupRestTemplate")
    public RestTemplate gupshupRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(GUPSHUP_BASE_URL)
                .setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                .additionalInterceptors(loggingInterceptor())
                .build();
    }
    
    /**
     * Logging interceptor for debugging Gupshup API calls
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("Gupshup API Request: {} {}", request.getMethod(), request.getURI());
            var response = execution.execute(request, body);
            log.debug("Gupshup API Response: {}", response.getStatusCode());
            return response;
        };
    }
}
