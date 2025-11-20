package com.immortals.platform.observability.logging;

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
 * Filter that adds correlation IDs to the MDC (Mapped Diagnostic Context).
 * Correlation IDs are used to track requests across service boundaries.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Get correlation ID from header or generate new one
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using existing correlation ID: {}", correlationId);
            }

            // Add correlation ID to MDC
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add correlation ID to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with the filter chain
            chain.doFilter(request, response);
        } finally {
            // Clean up MDC after request processing
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Generate a new correlation ID using UUID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
