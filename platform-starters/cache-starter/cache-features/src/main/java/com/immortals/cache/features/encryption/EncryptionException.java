package com.immortals.cache.features.encryption;

/**
 * Exception thrown when encryption or decryption operations fail.
 */
public class EncryptionException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public EncryptionException(String message) {
        super(message);
    }
    
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
