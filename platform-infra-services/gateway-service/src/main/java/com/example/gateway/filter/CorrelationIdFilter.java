package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter to add correlation IDs to all requests
 * This enables request tracking across multiple services
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        // Get existing correlation ID or generate a new one
        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        final String finalCorrelationId = correlationId;
        logger.debug("Processing request with correlation ID: {}", finalCorrelationId);
        
        // Add correlation ID to request headers
        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(builder -> builder.header(CORRELATION_ID_HEADER, finalCorrelationId))
            .build();
        
        // Add correlation ID to response headers
        modifiedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
        
        return chain.filter(modifiedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
