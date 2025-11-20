package com.immortals.usermanagementservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CountryDTO(

        @NotBlank(message = "Country name is required")
        @Size(min = 2, max = 100, message = "Country name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 5, message = "Country code must be between 2 and 5 characters")
        @Pattern(regexp = "^[A-Z]{2,5}$", message = "Country code must be uppercase letters (e.g., IN, USA)")
        String code,

        @NotNull(message = "Active indicator is required")
        Boolean activeInd,

        String userName

) {}
