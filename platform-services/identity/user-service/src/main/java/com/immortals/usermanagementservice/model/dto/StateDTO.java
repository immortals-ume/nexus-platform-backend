package com.immortals.usermanagementservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StateDTO(

        @NotBlank(message = "State name is required")
        @Size(min = 2, max = 100, message = "State name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "State code is required")
        @Size(min = 2, max = 10, message = "State code must be between 2 and 10 characters")
        @Pattern(regexp = "^[A-Z0-9]+$", message = "State code must be uppercase alphanumeric")
        String code,

        @NotNull(message = "Active indicator is required")
        Boolean activeInd,

        @NotNull(message = "Country Name is required")
        String countryName

) {}
