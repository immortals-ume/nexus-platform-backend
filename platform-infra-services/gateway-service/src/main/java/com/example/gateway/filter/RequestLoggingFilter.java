package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;

/**
 * Global filter for audit logging of all requests
 * Logs request details including method, path, IP address, and correlation ID
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Capture request start time
        exchange.getAttributes().put(REQUEST_START_TIME, Instant.now());
        
        // Extract request details
        HttpMethod method = request.getMethod();
        String path = request.getPath().value();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String ipAddress = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
        
        // Log incoming request
        logger.info("Incoming request: method={}, path={}, ip={}, correlationId={}", 
            method, path, ipAddress, correlationId);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Log response details
            int statusCode = exchange.getResponse().getStatusCode() != null 
                ? exchange.getResponse().getStatusCode().value() 
                : 0;
            
            Instant startTime = exchange.getAttribute(REQUEST_START_TIME);
            long duration = startTime != null 
                ? Instant.now().toEpochMilli() - startTime.toEpochMilli() 
                : 0;
            
            logger.info("Request completed: method={}, path={}, status={}, duration={}ms, correlationId={}", 
                method, path, statusCode, duration, correlationId);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
