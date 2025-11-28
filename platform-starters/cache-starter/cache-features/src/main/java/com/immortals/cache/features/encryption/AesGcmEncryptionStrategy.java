package com.immortals.cache.features.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption strategy implementation.
 * Provides authenticated encryption with associated data (AEAD).
 * <p>
 * AES-GCM is recommended for cache encryption as it provides both
 * confidentiality and authenticity without requiring separate MAC.
 */
public class AesGcmEncryptionStrategy implements EncryptionStrategy {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Creates an AES-GCM encryption strategy with the provided key.
     *
     * @param base64Key Base64-encoded AES key (must be 256 bits)
     */
    public AesGcmEncryptionStrategy(String base64Key) {
        if (base64Key == null || base64Key.trim()
                .isEmpty()) {
            throw new EncryptionException("Encryption key cannot be null or empty");
        }

        try {
            byte[] keyBytes = Base64.getDecoder()
                    .decode(base64Key);
            if (keyBytes.length != AES_KEY_SIZE / 8) {
                throw new EncryptionException(
                        String.format("Invalid key size: expected %d bytes, got %d bytes",
                                AES_KEY_SIZE / 8, keyBytes.length)
                );
            }
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            this.secureRandom = new SecureRandom();
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("Invalid Base64-encoded key", e);
        }
    }

    /**
     * Generates a new random AES-256 key.
     *
     * @return Base64-encoded key
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(AES_KEY_SIZE);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder()
                    .encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionException("Failed to generate encryption key", e);
        }
    }

    @Override
    public byte[] encrypt(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(data);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            return byteBuffer.array();

        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt data using AES-GCM", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }

        if (data.length < GCM_IV_LENGTH) {
            throw new EncryptionException("Invalid encrypted data: too short");
        }

        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return cipher.doFinal(encryptedData);

        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt data using AES-GCM", e);
        }
    }

    @Override
    public String getAlgorithm() {
        return "AES-GCM";
    }

    @Override
    public void validateConfiguration() {
        try {
            byte[] testData = "validation-test".getBytes();
            byte[] encrypted = encrypt(testData);
            byte[] decrypted = decrypt(encrypted);

            if (!java.util.Arrays.equals(testData, decrypted)) {
                throw new EncryptionException("Encryption validation failed: decrypted data does not match original");
            }
        } catch (EncryptionException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Encryption configuration validation failed", e);
        }
    }
}
