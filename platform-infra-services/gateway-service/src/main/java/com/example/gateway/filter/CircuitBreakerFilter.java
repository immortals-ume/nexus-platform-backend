package com.example.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to monitor circuit breaker states
 * Logs circuit breaker state changes for all downstream services
 */
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFilter.class);
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerFilter(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        registerCircuitBreakerEventListeners();
    }

    private void registerCircuitBreakerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    logger.warn("Circuit breaker state transition: name={}, from={}, to={}", 
                        circuitBreaker.getName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState());
                })
                .onError(event -> {
                    logger.error("Circuit breaker error: name={}, error={}", 
                        circuitBreaker.getName(),
                        event.getThrowable().getMessage());
                })
                .onSuccess(event -> {
                    logger.debug("Circuit breaker success: name={}, duration={}ms", 
                        circuitBreaker.getName(),
                        event.getElapsedDuration().toMillis());
                });
        });
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // This filter primarily monitors circuit breaker events
        // The actual circuit breaking is handled by Spring Cloud Gateway's CircuitBreaker filter
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
