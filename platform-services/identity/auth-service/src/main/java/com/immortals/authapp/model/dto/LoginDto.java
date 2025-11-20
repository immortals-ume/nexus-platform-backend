package com.immortals.authapp.model.dto;


public record LoginDto(
        String username,
        String password,
        Boolean rememberMe
) {}

