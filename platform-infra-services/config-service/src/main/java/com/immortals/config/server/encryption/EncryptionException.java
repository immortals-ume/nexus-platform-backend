package com.immortals.config.server.encryption;

public class EncryptionException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public EncryptionException(String message) {
        super(message);
    }
    
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
