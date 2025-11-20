package com.immortals.authapp.controller.exception;

import com.immortals.platform.domain.helper.ErrorDto;
import com.immortals.authapp.service.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;


@Slf4j
@ControllerAdvice
public class AuthGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public AuthGlobalExceptionHandler() {
        super();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NotNull MethodArgumentNotValidException methodArgumentNotValidException, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        logger.error(methodArgumentNotValidException.getClass()
                .getName() + "Encountered" + " " + methodArgumentNotValidException);

        final List<String> errors = new ArrayList<>();
        for (final FieldError error : methodArgumentNotValidException.getBindingResult()
                .getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }
        for (final ObjectError error : methodArgumentNotValidException.getBindingResult()
                .getGlobalErrors()) {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        }
        final ErrorDto errorDto = new ErrorDto(HttpStatus.BAD_REQUEST, methodArgumentNotValidException.getLocalizedMessage(), errors, request.getContextPath());
        return handleExceptionInternal(methodArgumentNotValidException, errorDto, headers, errorDto.getStatus(), request);
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(@NotNull TypeMismatchException typeMismatchException, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        logger.error(typeMismatchException.getClass()
                .getName() + "Encountered" + " " + typeMismatchException);
        final String error = typeMismatchException.getValue() + " value for " + typeMismatchException.getPropertyName() + " should be of type " + typeMismatchException.getRequiredType();

        final ErrorDto errorDto = new ErrorDto(HttpStatus.BAD_REQUEST, typeMismatchException.getLocalizedMessage(), Collections.singletonList(error), request.getContextPath());
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(@NotNull MissingServletRequestParameterException missingServletRequestParameterException, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        logger.error(missingServletRequestParameterException.getClass()
                .getName() + "Encountered" + " " + missingServletRequestParameterException);

        final String error = missingServletRequestParameterException.getParameterName() + " parameter is missing";
        final ErrorDto errorDto = new ErrorDto(HttpStatus.BAD_REQUEST, missingServletRequestParameterException.getLocalizedMessage(), Collections.singletonList(error), request.getContextPath());
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(@NotNull MissingPathVariableException missingPathVariableException, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        logger.error(missingPathVariableException.getClass()
                .getName() + "Encountered" + " " + missingPathVariableException);

        final String error = missingPathVariableException.getParameter() + " path Variable is missing";
        final ErrorDto errorDto = new ErrorDto(HttpStatus.BAD_REQUEST, missingPathVariableException.getLocalizedMessage(), Collections.singletonList(error), request.getContextPath());
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorDto> handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
        logger.error(methodArgumentTypeMismatchException.getClass()
                .getName() + "Encountered" + " " + methodArgumentTypeMismatchException);
        final String error = methodArgumentTypeMismatchException.getName() + " should be of type " + Objects.requireNonNull(methodArgumentTypeMismatchException.getRequiredType())
                .getName();

        final ErrorDto errorDto = new ErrorDto(HttpStatus.BAD_REQUEST, methodArgumentTypeMismatchException.getLocalizedMessage(), Collections.singletonList(error), null);
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<ErrorDto> handleConstraintViolation(final ConstraintViolationException constraintViolationException) {
        logger.error(constraintViolationException.getClass()
                .getName() + "Encountered" + " " + constraintViolationException);
        final List<String> errors = new ArrayList<>();
        for (final ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
            errors.add(violation.getRootBeanClass()
                    .getName() + " " + violation.getPropertyPath() + ": " + violation.getMessage());
        }

        final ErrorDto errorDto = new ErrorDto(HttpStatus.BAD_REQUEST, constraintViolationException.getLocalizedMessage(), errors, null);
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }


    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(@NotNull NoHandlerFoundException noHandlerFoundException, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        final String error = "No handler found for " + noHandlerFoundException.getHttpMethod() + " " + noHandlerFoundException.getRequestURL();

        logger.error(noHandlerFoundException.getClass()
                .getName() + "Encountered" + " " + noHandlerFoundException);

        final ErrorDto errorDto = new ErrorDto(HttpStatus.NOT_FOUND, noHandlerFoundException.getLocalizedMessage(), Collections.singletonList(error), request.getContextPath());
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException httpRequestMethodNotSupportedException, @NotNull HttpHeaders headers, @NotNull HttpStatusCode status, @NotNull WebRequest request) {
        final StringBuilder error = new StringBuilder();
        error.append(httpRequestMethodNotSupportedException.getMethod());
        error.append(" method is not supported for this request. Supported methods are ");
        Objects.requireNonNull(httpRequestMethodNotSupportedException.getSupportedHttpMethods())
                .forEach(t -> error.append(t)
                        .append(" "));

        final ErrorDto errorDto = new ErrorDto(HttpStatus.METHOD_NOT_ALLOWED, httpRequestMethodNotSupportedException.getLocalizedMessage(), Collections.singletonList(error.toString()), request.getContextPath());
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorDto> handleAll(final Exception e, HttpServletRequest request) {
        logger.error(e.getClass()
                .getName() + "Encountered" + " " + e.getLocalizedMessage());
        final ErrorDto errorDto = new ErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), null, request.getRequestURI());
        return new ResponseEntity<>(errorDto, new HttpHeaders(), errorDto.getStatus());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

}