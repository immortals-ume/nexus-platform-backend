package com.immortals.config.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for encryption operations
 * Includes validation constraints to prevent malicious inputs
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionRequest {
    
    /**
     * The value to encrypt/decrypt
     * Must not be null, blank, or exceed maximum size
     */
    @NotNull(message = "Value cannot be null")
    @NotBlank(message = "Value cannot be blank")
    @Size(max = 10000, message = "Value cannot exceed 10000 characters")
    private String value;
}
