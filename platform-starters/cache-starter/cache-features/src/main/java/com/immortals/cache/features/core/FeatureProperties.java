package com.immortals.cache.features.core;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Feature configuration.
 */
@Data
public class FeatureProperties {
    /**
     * Compression configuration.
     */
    private CompressionProperties compression = new CompressionProperties();

    /**
     * Encryption configuration.
     */
    private EncryptionProperties encryption = new EncryptionProperties();

    /**
     * Serialization configuration.
     */
    private SerializationProperties serialization = new SerializationProperties();

    /**
     * Compression configuration.
     */
    @Data
    public static class CompressionProperties {
        /**
         * Whether compression is enabled.
         * Default: false
         */
        private boolean enabled ;

        /**
         * Compression algorithm: gzip.
         * Default: gzip
         */
        private String algorithm ;

        /**
         * Minimum size in bytes to trigger compression.
         * Default: 1024 (1KB)
         */
        @Min(0)
        private int threshold ;
    }

    /**
     * Encryption configuration.
     */
    @Data
    public static class EncryptionProperties {
        /**
         * Whether encryption is enabled.
         * Default: false
         */
        private boolean enabled ;

        /**
         * Encryption algorithm: AES-GCM.
         * Default: AES-GCM
         */
        private String algorithm;

        /**
         * Base64-encoded encryption key.
         * Must be provided if encryption is enabled.
         */
        private String key;

        /**
         * Key size in bits: 128, 192, 256.
         * Default: 256
         */
        @Min(128)
        private int keySize ;
    }

    /**
     * Serialization configuration.
     */
    @Data
    public static class SerializationProperties {
        /**
         * Serialization strategy: jackson, java.
         * Default: jackson
         */
        private String strategy ;
    }
}