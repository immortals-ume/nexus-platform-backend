package com.example.gateway.controller;

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
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback(ServerWebExchange exchange) {
        logger.warn("Auth service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("auth-service", 
            "Authentication service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> usersFallback(ServerWebExchange exchange) {
        logger.warn("User management service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("user-management-service", 
            "User management service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationsFallback(ServerWebExchange exchange) {
        logger.warn("Notification service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("notification-service", 
            "Notification service is temporarily unavailable. Your notification will be processed later.");
    }

    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> storageFallback(ServerWebExchange exchange) {
        logger.warn("Storage service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("storage-service", 
            "Storage service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/otp")
    public ResponseEntity<Map<String, Object>> otpFallback(ServerWebExchange exchange) {
        logger.warn("OTP service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("otp-service", 
            "OTP service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/urls")
    public ResponseEntity<Map<String, Object>> urlsFallback(ServerWebExchange exchange) {
        logger.warn("URL shortener service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("url-shortener-service", 
            "URL shortener service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> customersFallback(ServerWebExchange exchange) {
        logger.warn("Customer service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("customer-service", 
            "Customer service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> ordersFallback(ServerWebExchange exchange) {
        logger.warn("Order service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("order-service", 
            "Order service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> paymentsFallback(ServerWebExchange exchange) {
        logger.warn("Payment service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("payment-service", 
            "Payment service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> productsFallback(ServerWebExchange exchange) {
        logger.warn("Product service fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("product-service", 
            "Product service is temporarily unavailable. Please try again later.");
    }

    /**
     * Generic fallback for any unavailable service
     */
    @GetMapping("/generic")
    public ResponseEntity<Map<String, Object>> genericFallback(ServerWebExchange exchange) {
        logger.warn("Generic fallback triggered for request: {}", 
            exchange.getRequest().getPath());
        return createFallbackResponse("service", 
            "The requested service is temporarily unavailable. Please try again later.");
    }

    /**
     * Creates a standardized fallback response
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("service", serviceName);
        response.put("fallback", true);
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(response);
    }
}
