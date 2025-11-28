package com.immortals.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Gateway configuration for rate limiting and other gateway features
 */
@Configuration
public class GatewayConfig {

    /**
     * Key resolver for rate limiting based on IP address
     * This ensures rate limiting is applied per IP address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ipAddress = Objects.requireNonNull(
                exchange.getRequest().getRemoteAddress()
            ).getAddress().getHostAddress();
            return Mono.just(ipAddress);
        };
    }
}
