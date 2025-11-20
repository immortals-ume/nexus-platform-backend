package com.immortals.usermanagementservice.model.dto;

public record UserResponseDTO(
        Long id,
        String fullName,
        String email,
        String address,
        String phoneNumber
) {
}