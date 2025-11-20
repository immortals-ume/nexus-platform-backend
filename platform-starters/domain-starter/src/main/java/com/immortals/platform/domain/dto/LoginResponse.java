package com.immortals.platform.domain.dto;

public record LoginResponse(String username,String accessToken,String refreshToken,String message) {
}
