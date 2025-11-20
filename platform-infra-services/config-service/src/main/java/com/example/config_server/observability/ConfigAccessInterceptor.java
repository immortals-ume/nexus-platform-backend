package com.example.config_server.observability;

import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigAccessInterceptor implements HandlerInterceptor {

    private final ConfigMetrics configMetrics;
    private final ConfigAuditLogger auditLogger;
    
    private static final String TIMER_ATTRIBUTE = "config.request.timer";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Start timer for request duration
        Timer.Sample sample = configMetrics.startTimer();
        request.setAttribute(TIMER_ATTRIBUTE, sample);
        
        // Record config request
        configMetrics.recordConfigRequest();
        
        // Extract configuration details from request path
        String path = request.getRequestURI();
        if (path.contains("/encrypt") || path.contains("/decrypt")) {
            if (path.contains("/encrypt")) {
                configMetrics.recordEncryptionRequest();
            } else {
                configMetrics.recordDecryptionRequest();
            }
        }
        
        // Log configuration access for audit
        if (isConfigRequest(path)) {
            String[] pathParts = path.split("/");
            String application = pathParts.length > 1 ? pathParts[1] : "unknown";
            String profile = pathParts.length > 2 ? pathParts[2] : "default";
            String label = pathParts.length > 3 ? pathParts[3] : "master";
            String clientIp = getClientIp(request);
            
            auditLogger.logConfigAccess(application, profile, label, clientIp);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // Stop timer
        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_ATTRIBUTE);
        if (sample != null) {
            configMetrics.stopTimer(sample);
        }
        
        // Record success or failure
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            configMetrics.recordConfigRequestSuccess();
        } else {
            configMetrics.recordConfigRequestFailure();
        }
    }

    private boolean isConfigRequest(String path) {
        return !path.contains("/actuator") && 
               !path.contains("/encrypt") && 
               !path.contains("/decrypt") &&
               !path.contains("/monitor");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
