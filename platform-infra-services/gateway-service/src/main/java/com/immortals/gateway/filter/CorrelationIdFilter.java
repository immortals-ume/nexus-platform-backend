package com.immortals.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Global filter to add correlation IDs to all requests
 * This enables request tracking across multiple services
 * Also adds correlation ID to MDC for logging
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        // Get existing correlation ID or generate a new one
        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            logger.debug("Generated new correlation ID: {}", correlationId);
        } else {
            logger.debug("Using existing correlation ID: {}", correlationId);
        }
        
        final String finalCorrelationId = correlationId;
        
        // Add correlation ID to request headers for downstream services
        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(builder -> builder.header(CORRELATION_ID_HEADER, finalCorrelationId))
            .build();
        
        // Add correlation ID to response headers
        modifiedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
        
        // Add correlation ID to reactor context for logging
        return chain.filter(modifiedExchange)
            .contextWrite(Context.of(CORRELATION_ID_KEY, finalCorrelationId))
            .doOnEach(signal -> {
                // Add to MDC for logging in reactive context
                if (signal.getContextView().hasKey(CORRELATION_ID_KEY)) {
                    MDC.put(CORRELATION_ID_KEY, signal.getContextView().get(CORRELATION_ID_KEY));
                }
            })
            .doFinally(signalType -> {
                // Clean up MDC after request processing
                MDC.remove(CORRELATION_ID_KEY);
            });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
