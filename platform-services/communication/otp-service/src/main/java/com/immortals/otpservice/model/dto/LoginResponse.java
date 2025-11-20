package com.immortals.otpservice.model.dto;

public record LoginResponse(String username,String accessToken,String refreshToken) {
}
