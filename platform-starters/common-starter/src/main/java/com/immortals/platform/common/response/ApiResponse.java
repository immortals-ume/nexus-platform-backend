package com.immortals.platform.common.response;

import com.immortals.platform.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
    int status,
    String statusText,
    boolean success,
    T data,
    String message,
    String errorCode,
    String path,
    Instant timestamp,
    String correlationId,
    List<ValidationError> validationErrors
) {
    
    public static <T> ApiResponse<T> informational(String message, String code) {
        return new ApiResponse<>(
            ErrorCode.getHttpStatus(code).value(),
            ErrorCode.getHttpStatus(code).getReasonPhrase(),
            true,
            null,
            message,
            code,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public static <T> ApiResponse<T> processing(String message) {
        return informational(message, ErrorCode.INFO_PROCESSING);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            HttpStatus.OK.getReasonPhrase(),
            true,
            data,
            null,
            ErrorCode.SUCCESS_OK,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            HttpStatus.OK.getReasonPhrase(),
            true,
            data,
            message,
            ErrorCode.SUCCESS_OK,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return new ApiResponse<>(
            HttpStatus.OK.value(),
            HttpStatus.OK.getReasonPhrase(),
            true,
            data,
            message,
            ErrorCode.SUCCESS_OK,
            null,
            Instant.now(),
            correlationId,
            null
        );
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(
            HttpStatus.CREATED.value(),
            HttpStatus.CREATED.getReasonPhrase(),
            true,
            data,
            message,
            ErrorCode.SUCCESS_CREATED,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public static <T> ApiResponse<T> accepted(String message) {
        return new ApiResponse<>(
            HttpStatus.ACCEPTED.value(),
            HttpStatus.ACCEPTED.getReasonPhrase(),
            true,
            null,
            message,
            ErrorCode.SUCCESS_ACCEPTED,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public static <T> ApiResponse<T> noContent(String message) {
        return new ApiResponse<>(
            HttpStatus.NO_CONTENT.value(),
            HttpStatus.NO_CONTENT.getReasonPhrase(),
            true,
            null,
            message,
            ErrorCode.SUCCESS_NO_CONTENT,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public static <T> ApiResponse<T> redirection(String message, String code) {
        return new ApiResponse<>(
            ErrorCode.getHttpStatus(code).value(),
            ErrorCode.getHttpStatus(code).getReasonPhrase(),
            false,
            null,
            message,
            code,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, String path, String correlationId) {
        HttpStatus status = ErrorCode.getHttpStatus(errorCode);
        return new ApiResponse<>(
            status.value(),
            status.getReasonPhrase(),
            false,
            null,
            message,
            errorCode,
            path,
            Instant.now(),
            correlationId,
            null
        );
    }

    public static <T> ApiResponse<T> error(int status, String statusText, String message, String path, String correlationId) {
        return new ApiResponse<>(
            status,
            statusText,
            false,
            null,
            message,
            null,
            path,
            Instant.now(),
            correlationId,
            null
        );
    }

    public static <T> ApiResponse<T> validationError(int status, String statusText, String message, String path, 
                                                     String correlationId, List<ValidationError> validationErrors) {
        return new ApiResponse<>(
            status,
            statusText,
            false,
            null,
            message,
            null,
            path,
            Instant.now(),
            correlationId,
            validationErrors
        );
    }

    public record ValidationError(
        String field,
        Object rejectedValue,
        String message,
        String code
    ) {
        public static ValidationError of(String field, Object rejectedValue, String message) {
            return new ValidationError(field, rejectedValue, message, null);
        }

        public static ValidationError of(String field, Object rejectedValue, String message, String code) {
            return new ValidationError(field, rejectedValue, message, code);
        }
    }
}