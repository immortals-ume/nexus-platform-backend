package com.immortals.authapp.model.dto;

public record UserResponseDTO(
        Long id,
        String fullName,
        String email,
        String address,
        String phoneNumber
) {
}