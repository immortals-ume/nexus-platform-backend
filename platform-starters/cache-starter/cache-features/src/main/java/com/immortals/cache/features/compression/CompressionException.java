package com.immortals.cache.features.compression;

import java.io.Serial;

/**
 * Exception thrown when compression or decompression operations fail.
 */
public class CompressionException extends RuntimeException {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    public CompressionException(String message) {
        super(message);
    }
    
    public CompressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
