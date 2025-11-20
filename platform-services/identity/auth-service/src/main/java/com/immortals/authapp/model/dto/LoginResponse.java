package com.immortals.authapp.model.dto;

public record LoginResponse(String username,String accessToken,String refreshToken,String message) {
}
