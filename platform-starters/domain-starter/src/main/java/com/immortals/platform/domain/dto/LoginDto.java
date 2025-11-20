package com.immortals.platform.domain.dto;


public record LoginDto(
        String username,
        String password,
        Boolean rememberMe
) {}

