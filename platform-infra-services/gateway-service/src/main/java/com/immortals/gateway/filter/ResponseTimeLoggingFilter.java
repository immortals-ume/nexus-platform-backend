package com.immortals.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Global filter to log and track response times
 * Also publishes metrics for monitoring
 */
@Component
public class ResponseTimeLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ResponseTimeLoggingFilter.class);
    private static final String REQUEST_START_TIME = "responseTimeStartTime";
    private final MeterRegistry meterRegistry;

    public ResponseTimeLoggingFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(REQUEST_START_TIME, Instant.now());
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Instant startTime = exchange.getAttribute(REQUEST_START_TIME);
            if (startTime != null) {
                Duration duration = Duration.between(startTime, Instant.now());
                long millis = duration.toMillis();
                
                String path = exchange.getRequest().getPath().value();
                String method = exchange.getRequest().getMethod() != null
                    ? exchange.getRequest().getMethod().name()
                    : "UNKNOWN";
                
                if (millis > 1000) {
                    logger.warn("Slow request detected: method={}, path={}, duration={}ms",
                        method, path, millis);
                } else {
                    logger.debug("Request completed: method={}, path={}, duration={}ms",
                        method, path, millis);
                }
                
                Timer.builder("gateway.request.duration")
                    .tag("method", method)
                    .tag("path", path)
                    .tag("status", String.valueOf(
                        exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0
                    ))
                    .register(meterRegistry)
                    .record(duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
