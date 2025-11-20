package com.example.config_server.encryption;

import com.example.config_server.observability.ConfigAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EncryptionController {

    private final EncryptionService encryptionService;
    private final ConfigAuditLogger auditLogger;

    /**
     * Encrypt a value using symmetric encryption
     * POST /encrypt with body containing the plain text
     */
    @PostMapping("/encrypt")
    public ResponseEntity<String> encrypt(@RequestBody String plainText, HttpServletRequest request) {
        log.info("Received encryption request");
        String clientIp = request.getRemoteAddr();
        try {
            String encrypted = encryptionService.encryptSymmetric(plainText);
            auditLogger.logEncryptionOperation("encrypt", clientIp, true);
            return ResponseEntity.ok(encrypted);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            auditLogger.logEncryptionOperation("encrypt", clientIp, false);
            return ResponseEntity.internalServerError().body("Encryption failed: " + e.getMessage());
        }
    }

    /**
     * Decrypt a value using symmetric encryption
     * POST /decrypt with body containing the encrypted text
     */
    @PostMapping("/decrypt")
    public ResponseEntity<String> decrypt(@RequestBody String encryptedText, HttpServletRequest request) {
        log.info("Received decryption request");
        String clientIp = request.getRemoteAddr();
        try {
            String decrypted = encryptionService.decryptSymmetric(encryptedText);
            auditLogger.logEncryptionOperation("decrypt", clientIp, true);
            return ResponseEntity.ok(decrypted);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            auditLogger.logEncryptionOperation("decrypt", clientIp, false);
            return ResponseEntity.internalServerError().body("Decryption failed: " + e.getMessage());
        }
    }

    /**
     * Encrypt a value using asymmetric encryption
     * POST /encrypt/asymmetric with body containing the plain text
     */
    @PostMapping("/encrypt/asymmetric")
    public ResponseEntity<String> encryptAsymmetric(@RequestBody String plainText) {
        log.info("Received asymmetric encryption request");
        try {
            if (!encryptionService.isAsymmetricEnabled()) {
                return ResponseEntity.badRequest().body("Asymmetric encryption not configured");
            }
            String encrypted = encryptionService.encryptAsymmetric(plainText);
            return ResponseEntity.ok(encrypted);
        } catch (Exception e) {
            log.error("Asymmetric encryption failed", e);
            return ResponseEntity.internalServerError().body("Asymmetric encryption failed: " + e.getMessage());
        }
    }

    /**
     * Decrypt a value using asymmetric encryption
     * POST /decrypt/asymmetric with body containing the encrypted text
     */
    @PostMapping("/decrypt/asymmetric")
    public ResponseEntity<String> decryptAsymmetric(@RequestBody String encryptedText) {
        log.info("Received asymmetric decryption request");
        try {
            if (!encryptionService.isAsymmetricEnabled()) {
                return ResponseEntity.badRequest().body("Asymmetric encryption not configured");
            }
            String decrypted = encryptionService.decryptAsymmetric(encryptedText);
            return ResponseEntity.ok(decrypted);
        } catch (Exception e) {
            log.error("Asymmetric decryption failed", e);
            return ResponseEntity.internalServerError().body("Asymmetric decryption failed: " + e.getMessage());
        }
    }

    /**
     * Get encryption status
     */
    @GetMapping("/encrypt/status")
    public ResponseEntity<Map<String, Object>> getEncryptionStatus() {
        return ResponseEntity.ok(Map.of(
            "symmetricEnabled", true,
            "asymmetricEnabled", encryptionService.isAsymmetricEnabled()
        ));
    }
}
