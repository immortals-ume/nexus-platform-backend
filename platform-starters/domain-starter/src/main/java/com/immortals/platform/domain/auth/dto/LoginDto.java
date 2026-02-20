package com.immortals.platform.domain.auth.dto;


public record LoginDto(
        String username,
        String password,
        Boolean rememberMe
) {}

