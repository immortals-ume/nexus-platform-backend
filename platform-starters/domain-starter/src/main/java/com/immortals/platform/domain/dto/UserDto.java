package com.immortals.platform.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record UserDto(@NotBlank String firstName, @NotBlank String middleName,
                      @NotBlank String lastName, @Size(min = 3, max = 16) @NotBlank String userName,
                      @Email(message = "Email is not in correct format") @NotBlank String email,
                      @NotBlank String phoneCode,
                      @Pattern(message = "Contact number invalid", regexp = "^(\\+91)?[6-9][0-9]{9}$") String contactNumber) implements Serializable {
}