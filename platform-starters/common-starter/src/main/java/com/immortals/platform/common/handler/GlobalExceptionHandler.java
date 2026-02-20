package com.immortals.platform.common.handler;

import com.immortals.platform.common.exception.*;
import com.immortals.platform.common.response.ApiResponse;
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

import java.util.List;
import java.util.stream.Collectors;

import static com.immortals.platform.common.constants.CommonConstant.CORRELATION_ID_HEADER;
import static com.immortals.platform.common.constants.CommonConstant.GENERIC_ERROR_MESSAGE;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RedirectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedirectionException(
            RedirectionException ex, HttpServletRequest request) {

        log.info("Redirection: {} to {}", ex.getMessage(), ex.getRedirectLocation());

        ApiResponse<Void> response = ApiResponse.redirection(ex.getMessage(), ex.getErrorCode());

        return ResponseEntity.status(ex.getHttpStatus())
                .header("Location", ex.getRedirectLocation())
                .body(response);
    }

    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlatformException(
            PlatformException ex, HttpServletRequest request) {

        log.warn("Platform exception: {} - {}", ex.getClass()
                .getSimpleName(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation failed: {}", ex.getMessage());

        List<ApiResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        ApiResponse<Void> response = ApiResponse.validationError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                getCorrelationId(request),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(com.immortals.platform.common.exception.ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            com.immortals.platform.common.exception.ValidationException ex, HttpServletRequest request) {

        log.warn("Validation exception: {}", ex.getMessage());

        ApiResponse<Void> response;

        if (ex.getValidationErrors() != null && !ex.getValidationErrors()
                .isEmpty()) {
            List<ApiResponse.ValidationError> validationErrors = ex.getValidationErrors()
                    .stream()
                    .map(ve -> ApiResponse.ValidationError.of(ve.getField(), ve.getRejectedValue(), ve.getMessage()))
                    .collect(Collectors.toList());

            response = ApiResponse.validationError(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    ex.getMessage(),
                    request.getRequestURI(),
                    getCorrelationId(request),
                    validationErrors
            );
        } else {
            response = ApiResponse.error(
                    ex.getMessage(),
                    ex.getErrorCode(),
                    request.getRequestURI(),
                    getCorrelationId(request)
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, HttpServletRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business exception: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(com.immortals.platform.common.exception.SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(
            com.immortals.platform.common.exception.SecurityException ex, HttpServletRequest request) {

        log.warn("Security exception: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiResponse<Void>> handleTechnicalException(
            TechnicalException ex, HttpServletRequest request) {

        log.error("Technical exception occurred", ex);

        ApiResponse<Void> response = ApiResponse.error(
                "A technical error occurred. Please try again later.",
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(ex.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("Constraint violation: {}", ex.getMessage());

        List<ApiResponse.ValidationError> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(this::mapConstraintViolation)
                .collect(Collectors.toList());

        ApiResponse<Void> response = ApiResponse.validationError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Constraint violation",
                request.getRequestURI(),
                getCorrelationId(request),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            TypeMismatchException ex, HttpServletRequest request) {

        log.warn("Type mismatch: {}", ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getPropertyName(),
                ex.getRequiredType() != null ? ex.getRequiredType()
                        .getSimpleName() : "unknown");

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Method argument type mismatch: {}", ex.getMessage());

        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType()
                        .getSimpleName() : "unknown");

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing request parameter: {}", ex.getMessage());

        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPathVariable(
            MissingPathVariableException ex, HttpServletRequest request) {

        log.warn("Missing path variable: {}", ex.getMessage());

        String message = String.format("Required path variable '%s' is missing", ex.getVariableName());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("No handler found: {}", ex.getMessage());

        String message = String.format("No handler found for %s %s",
                ex.getHttpMethod(), ex.getRequestURL());

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                message,
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("HTTP method not supported: {}", ex.getMessage());

        StringBuilder message = new StringBuilder();
        message.append(ex.getMethod())
                .append(" method is not supported for this request. ");
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods()
                .isEmpty()) {
            message.append("Supported methods are: ")
                    .append(ex.getSupportedHttpMethods()
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")));
        }

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                message.toString(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    @ExceptionHandler(CacheException.class)
    public ResponseEntity<ApiResponse<Void>> handleCacheException(
            CacheException ex, HttpServletRequest request) {

        log.error("Cache exception occurred", ex);

        ApiResponse<Void> response = ApiResponse.error(
                "A caching error occurred. Please try again later.",
                "CACHE_ERROR",
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseException(
            DatabaseException ex, HttpServletRequest request) {

        log.error("Database exception occurred", ex);

        ApiResponse<Void> response = ApiResponse.error(
                "A database error occurred. Please try again later.",
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOperation(
            InvalidOperationException ex, HttpServletRequest request) {

        log.warn("Invalid operation: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserException(
            UserException ex, HttpServletRequest request) {

        log.warn("User exception: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication exception: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected exception occurred", ex);

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                GENERIC_ERROR_MESSAGE,
                request.getRequestURI(),
                getCorrelationId(request)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private ApiResponse.ValidationError mapFieldError(FieldError fieldError) {
        return ApiResponse.ValidationError.of(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage(),
                fieldError.getCode()
        );
    }

    private ApiResponse.ValidationError mapConstraintViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath()
                .toString();
        return ApiResponse.ValidationError.of(
                field,
                violation.getInvalidValue(),
                violation.getMessage(),
                null
        );
    }

    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = (String) request.getAttribute(CORRELATION_ID_HEADER);
        }
        return correlationId;
    }
}
