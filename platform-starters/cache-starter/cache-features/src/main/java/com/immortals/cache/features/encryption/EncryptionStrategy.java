package com.immortals.cache.features.encryption;

/**
 * Strategy interface for encryption algorithms.
 * Implementations provide specific encryption/decryption logic.
 */
public interface EncryptionStrategy {

    /**
     * Encrypts the given data.
     *
     * @param data the data to encrypt
     * @return the encrypted data
     * @throws EncryptionException if encryption fails
     */
    byte[] encrypt(byte[] data);

    /**
     * Decrypts the given data.
     *
     * @param data the data to decrypt
     * @return the decrypted data
     * @throws EncryptionException if decryption fails
     */
    byte[] decrypt(byte[] data);

    /**
     * Returns the name of the encryption algorithm.
     *
     * @return the algorithm name (e.g., "AES-GCM", "AES-CBC")
     */
    String getAlgorithm();

    /**
     * Validates that the encryption configuration is correct.
     * Should be called at startup to fail fast if keys are invalid.
     *
     * @throws EncryptionException if validation fails
     */
    void validateConfiguration();
}
