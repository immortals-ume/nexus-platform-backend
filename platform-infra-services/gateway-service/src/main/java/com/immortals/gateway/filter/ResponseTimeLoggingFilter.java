package com.immortals.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ResponseTimeLoggingFilter implements GlobalFilter, Ordered {

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
                exchange.getRequest()
                        .getMethod();
                String method = exchange.getRequest().getMethod().name();
                
                if (millis > 1000) {
                    log.warn("Slow request detected: method={}, path={}, duration={}ms",
                        method, path, millis);
                } else {
                    log.debug("Request completed: method={}, path={}, duration={}ms",
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
