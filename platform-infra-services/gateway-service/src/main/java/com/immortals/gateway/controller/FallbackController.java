package com.immortals.gateway.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for circuit breaker fallbacks
 * Provides graceful degradation when downstream services are unavailable
 * Records metrics for fallback invocations
 */
@RestController
@RequestMapping("/fallback")
@RequiredArgsConstructor
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);
    private final MeterRegistry meterRegistry;

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Auth service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("auth-service",
            "Authentication service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> usersFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("User management service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("user-management-service",
            "User management service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationsFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Notification service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("notification-service",
            "Notification service is temporarily unavailable. Your notification will be processed later.", correlationId);
    }

    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> storageFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Storage service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("storage-service",
            "Storage service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/otp")
    public ResponseEntity<Map<String, Object>> otpFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("OTP service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("otp-service",
            "OTP service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/urls")
    public ResponseEntity<Map<String, Object>> urlsFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("URL shortener service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("url-shortener-service",
            "URL shortener service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> customersFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Customer service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("customer-service",
            "Customer service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> ordersFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Order service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("order-service",
            "Order service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> paymentsFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Payment service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("payment-service",
            "Payment service is temporarily unavailable. Please try again later.", correlationId);
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> productsFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Product service fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("product-service",
            "Product service is temporarily unavailable. Please try again later.", correlationId);
    }

    /**
     * Generic fallback for any unavailable service
     */
    @GetMapping("/generic")
    public ResponseEntity<Map<String, Object>> genericFallback(ServerWebExchange exchange) {
        String correlationId = getCorrelationId(exchange);
        logger.warn("Generic fallback triggered for request: {}, correlationId: {}",
            exchange.getRequest().getPath(), correlationId);
        return createFallbackResponse("service",
            "The requested service is temporarily unavailable. Please try again later.", correlationId);
    }

    /**
     * Creates a standardized fallback response
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName, String message, String correlationId) {
        // Record fallback invocation metric
        Counter.builder("gateway.fallback.invocations")
            .tag("service", serviceName)
            .register(meterRegistry)
            .increment();
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("service", serviceName);
        response.put("fallback", true);
        response.put("correlationId", correlationId);
        
        // Add Retry-After header (suggest retry after 30 seconds)
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "30")
            .header("X-Correlation-Id", correlationId)
            .body(response);
    }
    
    /**
     * Extracts correlation ID from request headers
     */
    private String getCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        return correlationId != null ? correlationId : "unknown";
    }
}
