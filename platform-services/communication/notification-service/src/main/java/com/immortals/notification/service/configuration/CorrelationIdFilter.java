package com.immortals.notification.service.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to add correlation ID to MDC for distributed tracing
 * Extracts correlation ID from request header or generates a new one
 * Adds correlation ID to response header for client tracking
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract or generate correlation ID
            String correlationId = extractOrGenerateCorrelationId(httpRequest);
            
            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            log.debug("Processing request with correlation ID: {}", correlationId);
            
            // Continue with the filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Clean up MDC
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Extract correlation ID from request header or generate a new one
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID from header: {}", correlationId);
        }
        
        return correlationId;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Initializing CorrelationIdFilter");
    }

    @Override
    public void destroy() {
        log.info("Destroying CorrelationIdFilter");
    }
}
