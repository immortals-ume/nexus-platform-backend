package com.example.config_server.webhook;

import com.example.config_server.observability.ConfigAuditLogger;
import com.example.config_server.observability.ConfigMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class GitWebhookController {

    private final ApplicationEventPublisher eventPublisher;
    private final ConfigMetrics configMetrics;
    private final ConfigAuditLogger auditLogger;

    /**
     * Webhook endpoint for Git repository changes
     * This endpoint is called by Git hosting services (GitHub, GitLab, Bitbucket)
     * when configuration files are updated
     * 
     * POST /monitor with Git webhook payload
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> handleGitWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String githubEvent,
            @RequestHeader(value = "X-GitLab-Event", required = false) String gitlabEvent) {
        
        log.info("Received Git webhook event");
        log.debug("Webhook payload: {}", payload);
        
        // Determine the source of the webhook
        String source = determineWebhookSource(githubEvent, gitlabEvent);
        log.info("Webhook source: {}", source);
        
        // Validate that this is a push event
        if (!isValidPushEvent(payload, source)) {
            log.warn("Ignoring non-push event from {}", source);
            return ResponseEntity.ok(Map.of(
                "status", "ignored",
                "message", "Not a push event"
            ));
        }
        
        try {
            // Broadcast refresh event to all services
            log.info("Configuration repository updated, broadcasting refresh event");
            RefreshRemoteApplicationEvent event = new RefreshRemoteApplicationEvent(
                this, 
                "config-server", 
                (org.springframework.cloud.bus.event.Destination) null
            );
            eventPublisher.publishEvent(event);
            configMetrics.recordRefreshEvent();
            auditLogger.logWebhookEvent(source, "push", true);
            auditLogger.logRefreshEvent("webhook", null);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Configuration refresh triggered",
                "source", source
            ));
        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            auditLogger.logWebhookEvent(source, "push", false);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to trigger refresh: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint for webhook configuration
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "Webhook endpoint is ready"
        ));
    }

    private String determineWebhookSource(String githubEvent, String gitlabEvent) {
        if (githubEvent != null) {
            return "GitHub";
        } else if (gitlabEvent != null) {
            return "GitLab";
        } else {
            return "Unknown";
        }
    }

    private boolean isValidPushEvent(Map<String, Object> payload, String source) {
        // GitHub sends push events with "ref" field
        if ("GitHub".equals(source)) {
            return payload.containsKey("ref") && payload.containsKey("commits");
        }
        
        // GitLab sends push events with "object_kind" = "push"
        if ("GitLab".equals(source)) {
            return "push".equals(payload.get("object_kind"));
        }
        
        // For unknown sources, check for common push event indicators
        return payload.containsKey("ref") || payload.containsKey("commits");
    }
}
