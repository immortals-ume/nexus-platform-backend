package com.immortals.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

/**
 * Global filter to add rate limit headers to responses
 * Provides clients with information about their rate limit status
 */
@Component
public class RateLimitHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitHeaderFilter.class);
    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    
    private static final int DEFAULT_RATE_LIMIT = 1000;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RateLimitHeaderFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ipAddress = Objects.requireNonNull(
            exchange.getRequest().getRemoteAddress()
        ).getAddress().getHostAddress();
        
        String rateLimitKey = "rate_limit:" + ipAddress;
        
        return redisTemplate.opsForValue()
            .get(rateLimitKey)
            .defaultIfEmpty("0")
            .flatMap(currentCount -> {
                int count = Integer.parseInt(currentCount);
                int remaining = Math.max(0, DEFAULT_RATE_LIMIT - count);
                
                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                long resetTime = ((currentTimeSeconds / 60) + 1) * 60;
                
                exchange.getResponse().getHeaders().add(RATE_LIMIT_HEADER, String.valueOf(DEFAULT_RATE_LIMIT));
                exchange.getResponse().getHeaders().add(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));
                exchange.getResponse().getHeaders().add(RATE_LIMIT_RESET_HEADER, String.valueOf(resetTime));
                
                if (remaining == 0) {
                    logger.warn("Rate limit exceeded for IP: {}, limit: {}", ipAddress, DEFAULT_RATE_LIMIT);
                }
                
                return chain.filter(exchange);
            })
            .onErrorResume(error -> {
                logger.error("Error checking rate limit for IP: {}, error: {}", ipAddress, error.getMessage());
                return chain.filter(exchange);
            });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
