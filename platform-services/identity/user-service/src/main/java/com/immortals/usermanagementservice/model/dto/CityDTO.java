package com.immortals.usermanagementservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CityDTO(

        @NotBlank(message = "City name is required")
        @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
        String name,

        @NotNull(message = "Active indicator is required")
        Boolean activeInd,

        @NotBlank(message = "State name is required")
        @Size(min = 2, max = 100, message = "State name must be between 2 and 100 characters")
        String stateName

) {}
