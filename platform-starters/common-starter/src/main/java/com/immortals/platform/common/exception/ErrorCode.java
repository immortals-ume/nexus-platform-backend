package com.immortals.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Standard codes for the platform with associated HTTP status codes.
 * Covers all HTTP status code ranges: 1xx, 2xx, 3xx, 4xx, 5xx
 * Follows the pattern: CATEGORY_SPECIFIC_CODE
 */
public final class ErrorCode {

    public static final String INFO_PROCESSING = "INFO_001";
    public static final String INFO_EARLY_HINTS = "INFO_002";
    
    public static final String SUCCESS_OK = "SUCCESS_001";
    public static final String SUCCESS_CREATED = "SUCCESS_002";
    public static final String SUCCESS_ACCEPTED = "SUCCESS_003";
    public static final String SUCCESS_NO_CONTENT = "SUCCESS_004";
    public static final String SUCCESS_PARTIAL_CONTENT = "SUCCESS_005";
    
    public static final String REDIRECT_MOVED_PERMANENTLY = "REDIRECT_001";
    public static final String REDIRECT_FOUND = "REDIRECT_002";
    public static final String REDIRECT_NOT_MODIFIED = "REDIRECT_003";
    public static final String REDIRECT_TEMPORARY = "REDIRECT_004";
    public static final String REDIRECT_PERMANENT = "REDIRECT_005";

    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_001";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_002";
    public static final String AUTH_ACCESS_DENIED = "AUTH_003";
    public static final String AUTH_ACCOUNT_LOCKED = "AUTH_004";
    public static final String AUTH_TOKEN_INVALID = "AUTH_005";

    public static final String BUSINESS_RULE_VIOLATION = "BUS_001";
    public static final String BUSINESS_INVALID_STATE = "BUS_002";
    public static final String BUSINESS_OPERATION_NOT_ALLOWED = "BUS_003";
    public static final String BUSINESS_CONSTRAINT_VIOLATION = "BUS_004";

    public static final String TECH_DATABASE_ERROR = "TECH_001";
    public static final String TECH_CACHE_ERROR = "TECH_002";
    public static final String TECH_EXTERNAL_SERVICE = "TECH_003";
    public static final String TECH_INTERNAL_ERROR = "TECH_004";
    public static final String TECH_SERVICE_UNAVAILABLE = "TECH_005";

    public static final String VALIDATION_REQUIRED_FIELD = "VAL_001";
    public static final String VALIDATION_INVALID_FORMAT = "VAL_002";
    public static final String VALIDATION_CONSTRAINT_VIOLATION = "VAL_003";
    public static final String VALIDATION_INVALID_INPUT = "VAL_004";

    public static final String RESOURCE_NOT_FOUND = "RES_001";
    public static final String RESOURCE_ALREADY_EXISTS = "RES_002";
    public static final String RESOURCE_CONFLICT = "RES_003";
    public static final String RESOURCE_GONE = "RES_004";
    
    public static final String SECURITY_ACCESS_DENIED = "SEC_001";
    public static final String SECURITY_INSUFFICIENT_PRIVILEGES = "SEC_002";
    public static final String SECURITY_FORBIDDEN_OPERATION = "SEC_003";

    public static final String USER_NOT_FOUND = "USER_001";
    public static final String USER_ALREADY_EXISTS = "USER_002";
    public static final String USER_INACTIVE = "USER_003";
    public static final String USER_INVALID_OPERATION = "USER_004";
    
    private ErrorCode() {
    }
    
    public static HttpStatus getHttpStatus(String code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        if (code.startsWith("INFO_")) {
            return switch (code) {
                case INFO_PROCESSING -> HttpStatus.PROCESSING;
                case INFO_EARLY_HINTS -> HttpStatus.valueOf(103);
                default -> HttpStatus.CONTINUE;
            };
        }
        
        if (code.startsWith("SUCCESS_")) {
            return switch (code) {
                case SUCCESS_OK -> HttpStatus.OK;
                case SUCCESS_CREATED -> HttpStatus.CREATED;
                case SUCCESS_ACCEPTED -> HttpStatus.ACCEPTED;
                case SUCCESS_NO_CONTENT -> HttpStatus.NO_CONTENT;
                case SUCCESS_PARTIAL_CONTENT -> HttpStatus.PARTIAL_CONTENT;
                default -> HttpStatus.OK;
            };
        }
        
        if (code.startsWith("REDIRECT_")) {
            return switch (code) {
                case REDIRECT_MOVED_PERMANENTLY -> HttpStatus.MOVED_PERMANENTLY;
                case REDIRECT_FOUND -> HttpStatus.FOUND;
                case REDIRECT_NOT_MODIFIED -> HttpStatus.NOT_MODIFIED;
                case REDIRECT_TEMPORARY -> HttpStatus.TEMPORARY_REDIRECT;
                case REDIRECT_PERMANENT -> HttpStatus.PERMANENT_REDIRECT;
                default -> HttpStatus.FOUND;
            };
        }

        if (code.startsWith("AUTH_")) {
            return switch (code) {
                case AUTH_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
                default -> HttpStatus.UNAUTHORIZED;
            };
        }
        
        if (code.startsWith("SEC_")) {
            return HttpStatus.FORBIDDEN;
        }
        
        if (code.startsWith("RES_")) {
            return switch (code) {
                case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
                case RESOURCE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
                case RESOURCE_CONFLICT -> HttpStatus.CONFLICT;
                case RESOURCE_GONE -> HttpStatus.GONE;
                default -> HttpStatus.BAD_REQUEST;
            };
        }
        
        if (code.startsWith("USER_")) {
            return switch (code) {
                case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
                case USER_ALREADY_EXISTS -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_REQUEST;
            };
        }
        
        if (code.startsWith("VAL_")) {
            return HttpStatus.BAD_REQUEST;
        }
    
        if (code.startsWith("BUS_")) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        
        if (code.startsWith("TECH_")) {
            return switch (code) {
                case TECH_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
        }
        
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}