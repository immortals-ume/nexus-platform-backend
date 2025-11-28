package com.immortals.config.server.encryption;

import com.immortals.config.server.dto.EncryptionRequest;
import com.immortals.config.server.dto.EncryptionResponse;
import com.immortals.config.server.observability.ConfigAuditLogger;
import com.immortals.config.server.util.InputSanitizer;
import com.immortals.config.server.observability.ConfigMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for custom encryption and decryption operations.
 * 
 * <p>Provides enhanced endpoints for encrypting and decrypting sensitive configuration values
 * using both symmetric and asymmetric algorithms. All operations are audited and
 * monitored for security and observability.</p>
 * 
 * <p><b>Endpoints:</b></p>
 * <ul>
 *   <li>{@code POST /api/encrypt} - Encrypt using symmetric encryption</li>
 *   <li>{@code POST /api/decrypt} - Decrypt using symmetric encryption</li>
 *   <li>{@code POST /api/encrypt/asymmetric} - Encrypt using asymmetric (RSA) encryption</li>
 *   <li>{@code POST /api/decrypt/asymmetric} - Decrypt using asymmetric (RSA) encryption</li>
 *   <li>{@code GET /api/encrypt/status} - Get encryption configuration status</li>
 * </ul>
 * 
 * <p><b>Security:</b></p>
 * <ul>
 *   <li>All inputs are validated and sanitized</li>
 *   <li>Operations are logged for audit purposes</li>
 *   <li>Metrics are recorded for monitoring</li>
 *   <li>Requires authentication (configured in SecurityConfig)</li>
 * </ul>
 * 
 * @author Platform Team
 * @version 1.0.0
 * @since 1.0.0
 * @see EncryptionService
 * @see ConfigAuditLogger
 * @see InputSanitizer
 */
@Slf4j
@RestController("customEncryptionController")
@RequestMapping("/api")
@RequiredArgsConstructor
public class EncryptionController {

    private final EncryptionService encryptionService;
    private final ConfigAuditLogger auditLogger;
    private final InputSanitizer inputSanitizer;
    private final ConfigMetrics configMetrics;

    /**
     * Encrypts a value using symmetric (AES) encryption.
     * 
     * <p>Example request:</p>
     * <pre>
     * POST /encrypt
     * Content-Type: application/json
     * 
     * {
     *   "value": "my-secret-password"
     * }
     * </pre>
     * 
     * @param encryptionRequest the request containing the value to encrypt
     * @param request the HTTP servlet request for audit logging
     * @return response containing the encrypted value
     * @throws EncryptionException if encryption fails
     */
    @PostMapping("/encrypt")
    public ResponseEntity<EncryptionResponse> encrypt(
            @Valid @RequestBody EncryptionRequest encryptionRequest,
            HttpServletRequest request) {
        log.info("Received encryption request");
        String clientIp = request.getRemoteAddr();
        inputSanitizer.validateOrThrow(encryptionRequest.getValue(), "value");
        
        String encrypted = encryptionService.encryptSymmetric(encryptionRequest.getValue());
        auditLogger.logEncryptionOperation("encrypt", clientIp, true);
        configMetrics.recordEncryptionRequest();
        
        return ResponseEntity.ok(new EncryptionResponse(encrypted, "success"));
    }

    /**
     * Decrypt a value using symmetric encryption
     * POST /decrypt with body containing the encrypted text
     */
    @PostMapping("/decrypt")
    public ResponseEntity<EncryptionResponse> decrypt(
            @Valid @RequestBody EncryptionRequest encryptionRequest,
            HttpServletRequest request) {
        log.info("Received decryption request");
        String clientIp = request.getRemoteAddr();

        inputSanitizer.validateOrThrow(encryptionRequest.getValue(), "value");
        
        String decrypted = encryptionService.decryptSymmetric(encryptionRequest.getValue());
        auditLogger.logEncryptionOperation("decrypt", clientIp, true);
        configMetrics.recordDecryptionRequest();
        
        return ResponseEntity.ok(new EncryptionResponse(decrypted, "success"));
    }

    /**
     * Encrypt a value using asymmetric encryption
     * POST /encrypt/asymmetric with body containing the plain text
     */
    @PostMapping("/encrypt/asymmetric")
    public ResponseEntity<EncryptionResponse> encryptAsymmetric(
            @Valid @RequestBody EncryptionRequest encryptionRequest,
            HttpServletRequest request) {
        log.info("Received asymmetric encryption request");
        String clientIp = request.getRemoteAddr();

        inputSanitizer.validateOrThrow(encryptionRequest.getValue(), "value");
        
        String encrypted = encryptionService.encryptAsymmetric(encryptionRequest.getValue());
        auditLogger.logEncryptionOperation("encrypt-asymmetric", clientIp, true);
        configMetrics.recordEncryptionRequest();
        
        return ResponseEntity.ok(new EncryptionResponse(encrypted, "success"));
    }

    /**
     * Decrypt a value using asymmetric encryption
     * POST /decrypt/asymmetric with body containing the encrypted text
     */
    @PostMapping("/decrypt/asymmetric")
    public ResponseEntity<EncryptionResponse> decryptAsymmetric(
            @Valid @RequestBody EncryptionRequest encryptionRequest,
            HttpServletRequest request) {
        log.info("Received asymmetric decryption request");
        String clientIp = request.getRemoteAddr();

        inputSanitizer.validateOrThrow(encryptionRequest.getValue(), "value");
        
        String decrypted = encryptionService.decryptAsymmetric(encryptionRequest.getValue());
        auditLogger.logEncryptionOperation("decrypt-asymmetric", clientIp, true);
        configMetrics.recordDecryptionRequest();
        
        return ResponseEntity.ok(new EncryptionResponse(decrypted, "success"));
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
