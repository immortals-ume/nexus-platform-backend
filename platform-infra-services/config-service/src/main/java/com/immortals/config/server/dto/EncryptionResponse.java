package com.immortals.config.server.dto;

import lombok.*;

/**
 * Response DTO for encryption operations
 */
@Getter
@Setter
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
