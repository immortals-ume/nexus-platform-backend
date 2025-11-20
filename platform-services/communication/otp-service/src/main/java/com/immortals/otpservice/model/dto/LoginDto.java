package com.immortals.otpservice.model.dto;


public record LoginDto(
        String username,
        String password,
        Boolean rememberMe
) {}

