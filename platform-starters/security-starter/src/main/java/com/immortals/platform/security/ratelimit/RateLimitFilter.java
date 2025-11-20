package com.immortals.platform.security.ratelimit;

import com.immortals.platform.security.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for rate limiting requests.
 * Supports both IP-based and user-based rate limiting.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final SecurityProperties securityProperties;

    public RateLimitFilter(RateLimiterService rateLimiterService,
                          SecurityProperties securityProperties) {
        this.rateLimiterService = rateLimiterService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        SecurityProperties.RateLimit rateLimitConfig = securityProperties.getRateLimit();
        
        if (!rateLimitConfig.getEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if path is excluded
        String requestPath = request.getRequestURI();
        if (isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String identifier = getIdentifier(request);
        
        if (!rateLimiterService.isAllowed(identifier)) {
            log.warn("Rate limit exceeded for identifier: {}, path: {}", identifier, requestPath);
            
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitConfig.getDefaultLimit()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + (rateLimitConfig.getTimeWindowSeconds() * 1000)));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        // Add rate limit headers
        int remaining = rateLimiterService.getRemainingTokens(identifier);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitConfig.getDefaultLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }

    /**
     * Get identifier for rate limiting (user ID or IP address).
     */
    private String getIdentifier(HttpServletRequest request) {
        SecurityProperties.RateLimit rateLimitConfig = securityProperties.getRateLimit();
        
        // Try user-based rate limiting first if enabled
        if (rateLimitConfig.getUserBased()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
                return "user:" + authentication.getName();
            }
        }

        // Fall back to IP-based rate limiting
        if (rateLimitConfig.getIpBased()) {
            String ipAddress = getClientIpAddress(request);
            return "ip:" + ipAddress;
        }

        // Default to IP if both are disabled (shouldn't happen)
        return "ip:" + getClientIpAddress(request);
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Check if path is excluded from rate limiting.
     */
    private boolean isExcludedPath(String path) {
        return securityProperties.getRateLimit().getExcludedPaths().stream()
                .anyMatch(path::startsWith);
    }
}
