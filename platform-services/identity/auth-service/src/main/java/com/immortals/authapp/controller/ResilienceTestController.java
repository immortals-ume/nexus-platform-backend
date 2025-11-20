package com.immortals.authapp.controller;

import com.immortals.authapp.client.OtpClient;
import com.immortals.authapp.service.ResilientDatabaseService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for testing and monitoring resilience patterns.
 * This controller provides endpoints to test circuit breakers, retries, and view their states.
 */
@RestController
@RequestMapping("/api/v1/resilience")
@RequiredArgsConstructor
@Slf4j
public class ResilienceTestController {

    private final OtpClient otpClient;
    private final ResilientDatabaseService resilientDatabaseService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * Test OTP service with resilience patterns.
     * This endpoint demonstrates circuit breaker, retry, timeout, and bulkhead in action.
     */
    @PostMapping("/test/otp/send")
    public ResponseEntity<Map<String, Object>> testOtpSend(@RequestParam String mobile) {
        log.info("Testing OTP send with resilience patterns for mobile: {}", mobile);
        
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = otpClient.sendOtp(mobile);
            response.put("success", result);
            response.put("message", result ? "OTP sent successfully" : "OTP send failed");
            response.put("mobile", mobile);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error testing OTP send", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            response.put("mobile", mobile);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Test database operations with resilience patterns.
     */
    @GetMapping("/test/database/user")
    public ResponseEntity<Map<String, Object>> testDatabaseOperation(@RequestParam String username) {
        log.info("Testing database operation with resilience patterns for username: {}", username);
        
        Map<String, Object> response = new HashMap<>();
        try {
            var user = resilientDatabaseService.findUserWithResilience(username);
            response.put("success", true);
            response.put("userFound", user.isPresent());
            response.put("username", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error testing database operation", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            response.put("username", username);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get circuit breaker states for monitoring.
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStates() {
        Map<String, Object> states = new HashMap<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbState = new HashMap<>();
            cbState.put("state", cb.getState().toString());
            cbState.put("metrics", Map.of(
                "failureRate", cb.getMetrics().getFailureRate(),
                "slowCallRate", cb.getMetrics().getSlowCallRate(),
                "numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls(),
                "numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls(),
                "numberOfSlowCalls", cb.getMetrics().getNumberOfSlowCalls(),
                "numberOfNotPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls()
            ));
            states.put(cb.getName(), cbState);
        });
        
        return ResponseEntity.ok(states);
    }

    /**
     * Get retry statistics for monitoring.
     */
    @GetMapping("/retries")
    public ResponseEntity<Map<String, Object>> getRetryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        retryRegistry.getAllRetries().forEach(retry -> {
            Map<String, Object> retryStats = new HashMap<>();
            retryStats.put("metrics", Map.of(
                "numberOfSuccessfulCallsWithoutRetryAttempt", 
                    retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                "numberOfSuccessfulCallsWithRetryAttempt", 
                    retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt(),
                "numberOfFailedCallsWithoutRetryAttempt", 
                    retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt(),
                "numberOfFailedCallsWithRetryAttempt", 
                    retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()
            ));
            stats.put(retry.getName(), retryStats);
        });
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Reset circuit breaker state (for testing purposes).
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<Map<String, String>> resetCircuitBreaker(@PathVariable String name) {
        try {
            var circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            circuitBreaker.reset();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Circuit breaker '" + name + "' has been reset");
            response.put("state", circuitBreaker.getState().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Circuit breaker '" + name + "' not found");
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * Transition circuit breaker to open state (for testing purposes).
     */
    @PostMapping("/circuit-breakers/{name}/open")
    public ResponseEntity<Map<String, String>> openCircuitBreaker(@PathVariable String name) {
        try {
            var circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            circuitBreaker.transitionToOpenState();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Circuit breaker '" + name + "' has been opened");
            response.put("state", circuitBreaker.getState().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Circuit breaker '" + name + "' not found");
            return ResponseEntity.status(404).body(response);
        }
    }

    /**
     * Transition circuit breaker to closed state (for testing purposes).
     */
    @PostMapping("/circuit-breakers/{name}/close")
    public ResponseEntity<Map<String, String>> closeCircuitBreaker(@PathVariable String name) {
        try {
            var circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            circuitBreaker.transitionToClosedState();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Circuit breaker '" + name + "' has been closed");
            response.put("state", circuitBreaker.getState().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Circuit breaker '" + name + "' not found");
            return ResponseEntity.status(404).body(response);
        }
    }
}
