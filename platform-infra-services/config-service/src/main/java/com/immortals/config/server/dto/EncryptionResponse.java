package com.immortals.config.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for encryption operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionResponse {
    
    /**
     * The encrypted/decrypted value
     */
    private String value;
    
    /**
     * Status of the operation
     */
    private String status;
}
