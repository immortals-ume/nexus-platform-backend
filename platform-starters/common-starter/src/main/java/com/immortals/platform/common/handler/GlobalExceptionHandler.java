package com.immortals.platform.common.handler;

import com.immortals.platform.common.exception.*;
import com.immortals.platform.common.model.ErrorResponse;
import com.immortals.platform.common.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.immortals.platform.common.constant.CommonConstant.CORRELATION_ID_HEADER;
import static com.immortals.platform.common.constant.CommonConstant.GENERIC_ERROR_MESSAGE;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across all services.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {



    /**
     * Handles ResourceNotFoundException (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles validation exceptions (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        log.warn("Validation failed: {}", ex.getMessage());
        
        List<ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                getCorrelationId(request),
                validationErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles BusinessException (400)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        log.warn("Business exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles SecurityException (401/403)
     */
    @ExceptionHandler(com.immortals.platform.common.exception.SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            com.immortals.platform.common.exception.SecurityException ex, HttpServletRequest request) {
        
        log.warn("Security exception: {}", ex.getMessage());

        HttpStatus status = ex.getMessage().toLowerCase().contains("authentication") 
                ? HttpStatus.UNAUTHORIZED 
                : HttpStatus.FORBIDDEN;
        
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles TechnicalException (500)
     */
    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponse> handleTechnicalException(
            TechnicalException ex, HttpServletRequest request) {
        
        log.error("Technical exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "A technical error occurred. Please try again later.",
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles ConstraintViolationException (400)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        log.warn("Constraint violation: {}", ex.getMessage());
        
        List<ValidationError> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(this::mapConstraintViolation)
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Constraint violation",
                request.getRequestURI(),
                getCorrelationId(request),
                validationErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles TypeMismatchException (400)
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            TypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Type mismatch: {}", ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getPropertyName(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MethodArgumentTypeMismatchException (400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Method argument type mismatch: {}", ex.getMessage());
        
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MissingServletRequestParameterException (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        log.warn("Missing request parameter: {}", ex.getMessage());
        
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MissingPathVariableException (400)
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariable(
            MissingPathVariableException ex, HttpServletRequest request) {
        
        log.warn("Missing path variable: {}", ex.getMessage());
        
        String message = String.format("Required path variable '%s' is missing", ex.getVariableName());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles NoHandlerFoundException (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        log.warn("No handler found: {}", ex.getMessage());
        
        String message = String.format("No handler found for %s %s", 
                ex.getHttpMethod(), ex.getRequestURL());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles HttpRequestMethodNotSupportedException (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        log.warn("HTTP method not supported: {}", ex.getMessage());
        
        StringBuilder message = new StringBuilder();
        message.append(ex.getMethod()).append(" method is not supported for this request. ");
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            message.append("Supported methods are: ")
                   .append(ex.getSupportedHttpMethods().stream()
                           .map(Object::toString)
                           .collect(Collectors.joining(", ")));
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                message.toString(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handles CacheException (500)
     */
    @ExceptionHandler(CacheException.class)
    public ResponseEntity<ErrorResponse> handleCacheException(
            CacheException ex, HttpServletRequest request) {
        
        log.error("Cache exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "A caching error occurred. Please try again later.",
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles DatabaseException (500)
     */
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(
            DatabaseException ex, HttpServletRequest request) {
        
        log.error("Database exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "A database error occurred. Please try again later.",
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles DuplicateResourceException (409)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles InvalidOperationException (400)
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex, HttpServletRequest request) {
        
        log.warn("Invalid operation: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles UserException (400)
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(
            UserException ex, HttpServletRequest request) {
        
        log.warn("User exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles AuthenticationException (401)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("Authentication exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        if (ex.getErrorCode() != null) {
            errorResponse.setErrorCode(ex.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles all other exceptions (500)
     * Does not expose internal implementation details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected exception occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                GENERIC_ERROR_MESSAGE,
                request.getRequestURI(),
                getCorrelationId(request)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Maps a Spring FieldError to our ValidationError model
     */
    private ValidationError mapFieldError(FieldError fieldError) {
        return ValidationError.of(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage(),
                fieldError.getCode()
        );
    }

    /**
     * Maps a ConstraintViolation to our ValidationError model
     */
    private ValidationError mapConstraintViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        return ValidationError.of(
                field,
                violation.getInvalidValue(),
                violation.getMessage(),
                null
        );
    }

    /**
     * Extracts correlation ID from request headers or attributes
     */
    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = (String) request.getAttribute(CORRELATION_ID_HEADER);
        }
        return correlationId;
    }
}
