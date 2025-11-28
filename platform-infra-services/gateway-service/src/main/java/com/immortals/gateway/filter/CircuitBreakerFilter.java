package com.immortals.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
 * Records metrics for circuit breaker activations
 */
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFilter.class);
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;

    public CircuitBreakerFilter(CircuitBreakerRegistry circuitBreakerRegistry, MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistry = meterRegistry;
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
                    
                    // Record metric for circuit breaker state transitions
                    Counter.builder("gateway.circuitbreaker.state.transitions")
                        .tag("name", circuitBreaker.getName())
                        .tag("from", event.getStateTransition().getFromState().name())
                        .tag("to", event.getStateTransition().getToState().name())
                        .register(meterRegistry)
                        .increment();
                    
                    // Record metric specifically for OPEN state (activation)
                    if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
                        Counter.builder("gateway.circuitbreaker.activations")
                            .tag("name", circuitBreaker.getName())
                            .register(meterRegistry)
                            .increment();
                    }
                })
                .onError(event -> {
                    logger.error("Circuit breaker error: name={}, error={}",
                        circuitBreaker.getName(),
                        event.getThrowable().getMessage());
                    
                    // Record error metric
                    Counter.builder("gateway.circuitbreaker.errors")
                        .tag("name", circuitBreaker.getName())
                        .register(meterRegistry)
                        .increment();
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
