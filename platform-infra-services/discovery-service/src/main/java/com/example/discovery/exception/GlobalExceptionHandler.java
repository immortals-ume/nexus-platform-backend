package com.example.discovery.exception;

import com.example.discovery.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for Config Service
 * Provides standardized error responses across all endpoints
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.warn("Validation error on request to {} with correlation ID {}: {}",
            request.getRequestURI(), getCorrelationId(request), ex.getMessage(), ex);
        
        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> ErrorResponse.ValidationError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .rejectedValue(error.getRejectedValue())
                .build())
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message("Input validation failed")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .correlationId(getCorrelationId(request))
            .validationErrors(validationErrors)
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions (e.g., from input sanitization)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        log.warn("Illegal argument on request to {} with correlation ID {}: {}",
            request.getRequestURI(), getCorrelationId(request), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Input")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .correlationId(getCorrelationId(request))
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    
    /**
     * Handle timeout exceptions
     */
    @ExceptionHandler({
        java.util.concurrent.TimeoutException.class,
        java.net.SocketTimeoutException.class
    })
    public ResponseEntity<ErrorResponse> handleTimeoutException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Timeout error on request to {} with correlation ID {}: {}",
            request.getRequestURI(), getCorrelationId(request), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.REQUEST_TIMEOUT.value())
            .error("Request Timeout")
            .message("The request exceeded the configured timeout duration")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .correlationId(getCorrelationId(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }
    
    /**
     * Handle connection exceptions
     */
    @ExceptionHandler({
        java.net.ConnectException.class,
        org.springframework.web.client.ResourceAccessException.class
    })
    public ResponseEntity<ErrorResponse> handleConnectionException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Connection error on request to {} with correlation ID {}: {}",
            request.getRequestURI(), getCorrelationId(request), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .error("Service Unavailable")
            .message("Unable to connect to downstream service")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .correlationId(getCorrelationId(request))
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error on request to {} with correlation ID {}: {}",
            request.getRequestURI(), getCorrelationId(request), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .correlationId(getCorrelationId(request))
            .build();
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
    
    /**
     * Extract correlation ID from request headers
     */
    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        return correlationId != null ? correlationId : "N/A";
    }
}
