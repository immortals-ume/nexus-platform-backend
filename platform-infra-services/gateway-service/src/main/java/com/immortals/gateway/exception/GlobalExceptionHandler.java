package com.immortals.gateway.exception;

import com.immortals.gateway.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler for Gateway Service (Reactive)
 * Provides standardized error responses across all endpoints
 */
@Slf4j
@Component
public class GlobalExceptionHandler extends DefaultErrorAttributes {
    
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Throwable error = getError(request);
        HttpStatus status = determineHttpStatus(error);
        String correlationId = getCorrelationId(request);
        
        log.error("Error on request to {} with correlation ID {}: {}",
            request.path(), correlationId, error.getMessage(), error);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(error.getMessage() != null ? error.getMessage() : "An unexpected error occurred")
            .path(request.path())
            .timestamp(LocalDateTime.now())
            .correlationId(correlationId)
            .build();
        
        return Map.of(
            "status", errorResponse.getStatus(),
            "error", errorResponse.getError(),
            "message", errorResponse.getMessage(),
            "path", errorResponse.getPath(),
            "timestamp", errorResponse.getTimestamp().toString(),
            "correlationId", errorResponse.getCorrelationId()
        );
    }
    
    /**
     * Determine HTTP status based on exception type
     */
    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        } else if (error instanceof java.util.concurrent.TimeoutException
                || error instanceof java.net.SocketTimeoutException) {
            return HttpStatus.REQUEST_TIMEOUT;
        } else if (error instanceof java.net.ConnectException
                || error.getMessage() != null && error.getMessage().contains("Connection refused")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    
    /**
     * Extract correlation ID from request headers
     */
    private String getCorrelationId(ServerRequest request) {
        return request.headers()
            .firstHeader("X-Correlation-ID") != null
            ? request.headers().firstHeader("X-Correlation-ID")
            : "N/A";
    }
}
