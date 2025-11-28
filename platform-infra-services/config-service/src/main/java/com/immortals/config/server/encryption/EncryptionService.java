package com.immortals.config.server.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive configuration values.
 * 
 * <p>Supports both symmetric (AES) and asymmetric (RSA) encryption algorithms.
 * Symmetric encryption is used for most configuration values, while asymmetric
 * encryption can be used when public key encryption is required.</p>
 * 
 * <p><b>Symmetric Encryption:</b></p>
 * <ul>
 *   <li>Algorithm: AES with Spring Security's TextEncryptor</li>
 *   <li>Configuration: {@code encrypt.key} property</li>
 *   <li>Use case: General configuration encryption</li>
 * </ul>
 * 
 * <p><b>Asymmetric Encryption:</b></p>
 * <ul>
 *   <li>Algorithm: RSA with PKCS1 padding</li>
 *   <li>Configuration: {@code encrypt.key-store.location} property</li>
 *   <li>Use case: Public key encryption scenarios</li>
 * </ul>
 * 
 * <p><b>Error Handling:</b></p>
 * All encryption/decryption operations validate inputs and throw {@link EncryptionException}
 * with descriptive messages on failure.
 * 
 * @author Platform Team
 * @version 1.0.0
 * @since 1.0.0
 * @see EncryptionController
 * @see EncryptionException
 */
@Slf4j
@Service
public class EncryptionService {

    private final TextEncryptor symmetricEncryptor;
    private final boolean asymmetricEnabled;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * Constructs an EncryptionService with symmetric and optional asymmetric encryption.
     * 
     * @param encryptKey the symmetric encryption key (from {@code encrypt.key} property)
     * @param keyStoreLocation the keystore location for asymmetric encryption (from {@code encrypt.key-store.location})
     */
    public EncryptionService(
            @Value("${encrypt.key:}") String encryptKey,
            @Value("${encrypt.key-store.location:}") String keyStoreLocation) {
        
        if (encryptKey != null && !encryptKey.isEmpty()) {
            this.symmetricEncryptor = Encryptors.text(encryptKey, "deadbeef");
            log.info("Symmetric encryption initialized");
        } else {
            this.symmetricEncryptor = Encryptors.noOpText();
            log.warn("No encryption key configured, encryption disabled");
        }
        
        this.asymmetricEnabled = keyStoreLocation != null && !keyStoreLocation.isEmpty();
        if (asymmetricEnabled) {
            log.info("Asymmetric encryption configured with keystore: {}", keyStoreLocation);
        }
    }

    /**
     * Encrypts a value using symmetric (AES) encryption.
     * 
     * <p>This method uses Spring Security's TextEncryptor with AES algorithm.
     * The encrypted value can be stored in configuration files and decrypted
     * when needed.</p>
     * 
     * @param plainText the plain text to encrypt (must not be null or empty)
     * @return the encrypted text as a Base64-encoded string
     * @throws EncryptionException if encryption fails or input is invalid
     * @throws IllegalArgumentException if plainText is null or empty
     */
    public String encryptSymmetric(String plainText) {
        validateInput(plainText, "Plain text");
        
        try {
            String encrypted = symmetricEncryptor.encrypt(plainText);
            log.debug("Successfully encrypted value using symmetric encryption");
            return encrypted;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for encryption: {}", e.getMessage());
            throw new EncryptionException("Invalid input for encryption: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to encrypt value", e);
            throw new EncryptionException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt a value using symmetric encryption
     * @param encryptedText the encrypted text to decrypt
     * @return the decrypted plain text
     * @throws EncryptionException if decryption fails or input is invalid
     */
    public String decryptSymmetric(String encryptedText) {
        validateInput(encryptedText, "Encrypted text");
        
        try {
            String decrypted = symmetricEncryptor.decrypt(encryptedText);
            log.debug("Successfully decrypted value using symmetric encryption");
            return decrypted;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for decryption: {}", e.getMessage());
            throw new EncryptionException("Invalid input for decryption: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to decrypt value", e);
            throw new EncryptionException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt a value using asymmetric encryption (RSA)
     * @param plainText the plain text to encrypt
     * @return the encrypted text
     * @throws EncryptionException if encryption fails, input is invalid, or asymmetric encryption is not configured
     */
    public String encryptAsymmetric(String plainText) {
        validateInput(plainText, "Plain text");
        
        if (!asymmetricEnabled || publicKey == null) {
            log.error("Asymmetric encryption not configured");
            throw new EncryptionException("Asymmetric encryption not configured");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            String encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            log.debug("Successfully encrypted value using asymmetric encryption");
            return encrypted;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for asymmetric encryption: {}", e.getMessage());
            throw new EncryptionException("Invalid input for asymmetric encryption: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to encrypt value with asymmetric encryption", e);
            throw new EncryptionException("Asymmetric encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt a value using asymmetric encryption (RSA)
     * @param encryptedText the encrypted text to decrypt
     * @return the decrypted plain text
     * @throws EncryptionException if decryption fails, input is invalid, or asymmetric encryption is not configured
     */
    public String decryptAsymmetric(String encryptedText) {
        validateInput(encryptedText, "Encrypted text");
        
        if (!asymmetricEnabled || privateKey == null) {
            log.error("Asymmetric encryption not configured");
            throw new EncryptionException("Asymmetric encryption not configured");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            String decrypted = new String(decryptedBytes);
            log.debug("Successfully decrypted value using asymmetric encryption");
            return decrypted;
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for asymmetric decryption: {}", e.getMessage());
            throw new EncryptionException("Invalid input for asymmetric decryption: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to decrypt value with asymmetric encryption", e);
            throw new EncryptionException("Asymmetric decryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate input for encryption/decryption operations
     * @param input the input to validate
     * @param fieldName the name of the field for error messages
     * @throws EncryptionException if input is null or empty
     */
    private void validateInput(String input, String fieldName) {
        if (input == null) {
            log.error("{} cannot be null", fieldName);
            throw new EncryptionException(fieldName + " cannot be null");
        }
        if (input.isEmpty()) {
            log.error("{} cannot be empty", fieldName);
            throw new EncryptionException(fieldName + " cannot be empty");
        }
    }

    /**
     * Load RSA key pair from keystore
     */
    public void loadKeyPair(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        log.info("RSA key pair loaded successfully");
    }

    public boolean isAsymmetricEnabled() {
        return asymmetricEnabled && privateKey != null && publicKey != null;
    }
}
