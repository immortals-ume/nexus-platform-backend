package com.immortals.authapp.model.dto;

import jakarta.validation.constraints.NotNull;

public record RegisterRequestDTO(
        @NotNull
        String firstName,
        @NotNull
        String middleName,
        @NotNull
        String lastName,
        @NotNull
        String userName,
        @NotNull
        String password,
        @NotNull
        String reTypePassword,

        @NotNull
        String email,
        @NotNull
        String phoneCode,
        @NotNull
        String contactNumber
) {
}


