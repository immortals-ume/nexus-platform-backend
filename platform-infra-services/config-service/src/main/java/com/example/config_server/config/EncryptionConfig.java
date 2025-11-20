package com.example.config_server.config;

import com.example.config_server.encryption.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

@Slf4j
@Configuration
public class EncryptionConfig {

    private final EncryptionService encryptionService;
    
    @Value("${encrypt.key-store.location:}")
    private String keyStoreLocation;
    
    @Value("${encrypt.key-store.password:}")
    private String keyStorePassword;
    
    @Value("${encrypt.key-store.alias:}")
    private String keyStoreAlias;
    
    @Value("${encrypt.key-store.secret:}")
    private String keyStoreSecret;

    public EncryptionConfig(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @PostConstruct
    public void init() {
        if (keyStoreLocation != null && !keyStoreLocation.isEmpty() && 
            !keyStoreLocation.equals("classpath:/config-server.jks")) {
            try {
                loadKeyStore();
            } catch (Exception e) {
                log.error("Failed to load keystore for asymmetric encryption", e);
            }
        } else {
            log.info("Asymmetric encryption keystore not configured, using symmetric encryption only");
        }
    }

    private void loadKeyStore() throws Exception {
        log.info("Loading keystore from: {}", keyStoreLocation);
        
        // Remove classpath: prefix if present
        String location = keyStoreLocation.replace("classpath:", "");
        
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(location)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }
        
        // Get private key
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(
            keyStoreAlias, 
            keyStoreSecret.toCharArray()
        );
        
        // Get public key from certificate
        Certificate certificate = keyStore.getCertificate(keyStoreAlias);
        PublicKey publicKey = certificate.getPublicKey();
        
        // Load keys into encryption service
        encryptionService.loadKeyPair(privateKey, publicKey);
        
        log.info("Successfully loaded RSA key pair from keystore");
    }
}
