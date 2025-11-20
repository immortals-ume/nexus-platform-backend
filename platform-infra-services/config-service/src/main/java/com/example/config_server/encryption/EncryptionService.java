package com.example.config_server.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {

    private final TextEncryptor symmetricEncryptor;
    private final boolean asymmetricEnabled;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public EncryptionService(
            @Value("${encrypt.key:}") String encryptKey,
            @Value("${encrypt.key-store.location:}") String keyStoreLocation) {
        
        // Initialize symmetric encryption
        if (encryptKey != null && !encryptKey.isEmpty()) {
            this.symmetricEncryptor = Encryptors.text(encryptKey, "deadbeef");
            log.info("Symmetric encryption initialized");
        } else {
            this.symmetricEncryptor = Encryptors.noOpText();
            log.warn("No encryption key configured, encryption disabled");
        }
        
        // Check if asymmetric encryption is configured
        this.asymmetricEnabled = keyStoreLocation != null && !keyStoreLocation.isEmpty();
        if (asymmetricEnabled) {
            log.info("Asymmetric encryption configured with keystore: {}", keyStoreLocation);
        }
    }

    /**
     * Encrypt a value using symmetric encryption
     */
    public String encryptSymmetric(String plainText) {
        try {
            String encrypted = symmetricEncryptor.encrypt(plainText);
            log.debug("Successfully encrypted value using symmetric encryption");
            return encrypted;
        } catch (Exception e) {
            log.error("Failed to encrypt value", e);
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypt a value using symmetric encryption
     */
    public String decryptSymmetric(String encryptedText) {
        try {
            String decrypted = symmetricEncryptor.decrypt(encryptedText);
            log.debug("Successfully decrypted value using symmetric encryption");
            return decrypted;
        } catch (Exception e) {
            log.error("Failed to decrypt value", e);
            throw new EncryptionException("Decryption failed", e);
        }
    }

    /**
     * Encrypt a value using asymmetric encryption (RSA)
     */
    public String encryptAsymmetric(String plainText) {
        if (!asymmetricEnabled || publicKey == null) {
            throw new EncryptionException("Asymmetric encryption not configured");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            String encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            log.debug("Successfully encrypted value using asymmetric encryption");
            return encrypted;
        } catch (Exception e) {
            log.error("Failed to encrypt value with asymmetric encryption", e);
            throw new EncryptionException("Asymmetric encryption failed", e);
        }
    }

    /**
     * Decrypt a value using asymmetric encryption (RSA)
     */
    public String decryptAsymmetric(String encryptedText) {
        if (!asymmetricEnabled || privateKey == null) {
            throw new EncryptionException("Asymmetric encryption not configured");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            String decrypted = new String(decryptedBytes);
            log.debug("Successfully decrypted value using asymmetric encryption");
            return decrypted;
        } catch (Exception e) {
            log.error("Failed to decrypt value with asymmetric encryption", e);
            throw new EncryptionException("Asymmetric decryption failed", e);
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
