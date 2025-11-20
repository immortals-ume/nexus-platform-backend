package com.immortals.usermanagementservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @Size(max = 50, message = "Middle name can be up to 50 characters")
        String middleName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        String userName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be at least 8 characters long")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
                message = "Password must contain at least one uppercase, one lowercase, one number, and one special character"
        )
        String password,

        @NotBlank(message = "Re-type password is required")
        String reTypePassword,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone code is required")
        @Pattern(regexp = "^\\+?[0-9]{1,4}$", message = "Invalid phone code format")
        String phoneCode,

        @NotBlank(message = "Contact number is required")
        @Pattern(regexp = "^[0-9]{7,15}$", message = "Contact number must be 7–15 digits")
        String contactNumber

) { }
