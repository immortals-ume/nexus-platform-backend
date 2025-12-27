package com.immortals.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfiguration {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return Mono.just(authHeader.substring(7, Math.min(authHeader.length(), 50)));
            }
            
            String remoteAddr = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(remoteAddr);
        };
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String remoteAddr = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(remoteAddr);
        };
    }

    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }
}
