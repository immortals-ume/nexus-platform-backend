package com.immortals.usermanagementservice.constants;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthAppConstant {
    public static final String MOBILE_REGEX_INDIA = "^[6-9]\\\\d{9}$";
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[#$@!%&*?])[A-Za-z\\d#$@!%&*?]{8,16}$";
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String MDC_USER_AGENT_KEY = "userAgent";
    public static final Long MAX_AGE_CORS_SECS = 86400L;
    public static final Long MAX_AGE_HSTS_SECS = 86400L;

    public static final int MAX_TOKENS = 100;
    public static final int REFILL_TOKENS_PER_SECONDS = 10;

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    public static final String[] PUBLIC_RESOURCE_PATHS = {
            "/redirect",
            "/health",
            "/actuator/**",
            "/static/**",
            "/webjars/**",
            "/favicon.ico",
            "/css/**",
            "/js/**",
            "/images/**"
    };

    public static final String[] ANONYMOUS_API_PATHS = {
            "/api/v1/users/register",
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/remember-me/"
    };
}
