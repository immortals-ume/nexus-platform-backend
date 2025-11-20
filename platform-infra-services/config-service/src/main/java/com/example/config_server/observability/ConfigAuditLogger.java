package com.example.config_server.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ConfigAuditLogger {

    /**
     * Log configuration access for audit purposes
     */
    public void logConfigAccess(String application, String profile, String label, String clientIp) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("event", "config_access");
        auditLog.put("timestamp", Instant.now().toString());
        auditLog.put("application", application);
        auditLog.put("profile", profile);
        auditLog.put("label", label);
        auditLog.put("client_ip", clientIp);
        
        log.info("Config access audit: {}", auditLog);
    }

    /**
     * Log encryption operation for audit purposes
     */
    public void logEncryptionOperation(String operation, String clientIp, boolean success) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("event", "encryption_operation");
        auditLog.put("timestamp", Instant.now().toString());
        auditLog.put("operation", operation);
        auditLog.put("client_ip", clientIp);
        auditLog.put("success", success);
        
        log.info("Encryption operation audit: {}", auditLog);
    }

    /**
     * Log configuration refresh event for audit purposes
     */
    public void logRefreshEvent(String source, String targetService) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("event", "config_refresh");
        auditLog.put("timestamp", Instant.now().toString());
        auditLog.put("source", source);
        auditLog.put("target_service", targetService != null ? targetService : "all");
        
        log.info("Config refresh audit: {}", auditLog);
    }

    /**
     * Log webhook event for audit purposes
     */
    public void logWebhookEvent(String source, String event, boolean success) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("event", "webhook_received");
        auditLog.put("timestamp", Instant.now().toString());
        auditLog.put("source", source);
        auditLog.put("webhook_event", event);
        auditLog.put("success", success);
        
        log.info("Webhook event audit: {}", auditLog);
    }

    /**
     * Log configuration error for audit purposes
     */
    public void logConfigError(String application, String profile, String error) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("event", "config_error");
        auditLog.put("timestamp", Instant.now().toString());
        auditLog.put("application", application);
        auditLog.put("profile", profile);
        auditLog.put("error", error);
        
        log.error("Config error audit: {}", auditLog);
    }
}
