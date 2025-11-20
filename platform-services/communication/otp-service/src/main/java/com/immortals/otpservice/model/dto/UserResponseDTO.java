package com.immortals.otpservice.model.dto;

public record UserResponseDTO(
        Long id,
        String fullName,
        String email,
        String address,
        String phoneNumber
) {
}