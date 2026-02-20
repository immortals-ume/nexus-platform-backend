package com.immortals.platform.domain.dto;

public record UserAddressDTO(
        String addressLine1,
        String addressLine2,
        Long city,
        Long state,
        Long country,
        String zipCode,
        String addressStatus
) {}
