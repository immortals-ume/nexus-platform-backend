package com.immortals.config.server.refresh;

import com.immortals.config.server.observability.ConfigAuditLogger;
import com.immortals.config.server.observability.ConfigMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@SuppressWarnings("")
public class RefreshController {

    private final ApplicationEventPublisher eventPublisher;
    private final ConfigMetrics configMetrics;
    private final ConfigAuditLogger auditLogger;

    /**
     * Trigger refresh for all services via Spring Cloud Bus
     * POST /actuator/busrefresh
     */
    @PostMapping("/actuator/busrefresh")
    public ResponseEntity<Map<String, String>> refreshAll() {
        log.info("Broadcasting refresh event to all services");
        try {
            RefreshRemoteApplicationEvent event = new RefreshRemoteApplicationEvent(
                    this,
                    "config-server",
                    (org.springframework.cloud.bus.event.Destination) null
            );
            eventPublisher.publishEvent(event);
            configMetrics.recordRefreshEvent();
            auditLogger.logRefreshEvent("manual", null);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Refresh event broadcasted to all services"
            ));
        } catch (Exception e) {
            log.error("Failed to broadcast refresh event", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to broadcast refresh: " + e.getMessage()
                    ));
        }
    }

    /**
     * Trigger refresh for a specific service via Spring Cloud Bus
     * POST /actuator/busrefresh/{service}
     */
    @PostMapping("/actuator/busrefresh/{service}")
    public ResponseEntity<Map<String, String>> refreshService(@PathVariable String service) {
        log.info("Broadcasting refresh event to service: {}", service);
        try {
            RefreshRemoteApplicationEvent event = new RefreshRemoteApplicationEvent(
                    this,
                    "config-server",
                    () -> "service"
            );
            eventPublisher.publishEvent(event);
            configMetrics.recordRefreshEvent();
            auditLogger.logRefreshEvent("manual", service);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Refresh event broadcasted to service: " + service
            ));
        } catch (Exception e) {
            log.error("Failed to broadcast refresh event to service: {}", service, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to broadcast refresh: " + e.getMessage()
                    ));
        }
    }
}
