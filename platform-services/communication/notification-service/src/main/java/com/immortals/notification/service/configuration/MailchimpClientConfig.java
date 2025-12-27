package com.immortals.notification.service.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for Mailchimp Transactional API HTTP client
 * Provides RestTemplate bean configured for Mailchimp API calls
 */
@Slf4j
@Configuration
public class MailchimpClientConfig {
    
    private static final String MAILCHIMP_API_BASE_URL = "https://mandrillapp.com/api/1.0";
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 30;
    
    /**
     * Create RestTemplate configured for Mailchimp Transactional API (Mandrill)
     * 
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @return configured RestTemplate for Mailchimp API calls
     */
    @Bean(name = "mailchimpRestTemplate")
    public RestTemplate mailchimpRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring Mailchimp RestTemplate with base URL: {}", MAILCHIMP_API_BASE_URL);
        
        return builder
            .rootUri(MAILCHIMP_API_BASE_URL)
            .setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            .additionalInterceptors(loggingInterceptor())
            .build();
    }
    
    /**
     * Create logging interceptor for debugging API calls
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("Mailchimp API Request: {} {}", request.getMethod(), request.getURI());
            return execution.execute(request, body);
        };
    }
}
