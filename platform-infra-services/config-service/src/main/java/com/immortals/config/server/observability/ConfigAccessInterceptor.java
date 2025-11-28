package com.immortals.config.server.observability;

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
        Timer.Sample sample = configMetrics.startTimer();
        request.setAttribute(TIMER_ATTRIBUTE, sample);
        
        configMetrics.recordConfigRequest();
        
        String path = request.getRequestURI();
        if (path.contains("/encrypt") || path.contains("/decrypt")) {
            if (path.contains("/encrypt")) {
                configMetrics.recordEncryptionRequest();
            } else {
                configMetrics.recordDecryptionRequest();
            }
        }
        
        if (isConfigRequest(path)) {
            String[] pathParts = path.split("/");
            String application = pathParts.length > 1 ? pathParts[1] : "unknown";
            String profile = pathParts.length > 2 ? pathParts[2] : "default";
            String label = pathParts.length > 3 ? pathParts[3] : "master";
            String clientIp = getClientIp(request);
            String user = getUserIdentity(request);
            
            auditLogger.logConfigAccess(application, profile, label, clientIp, user);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_ATTRIBUTE);
        if (sample != null) {
            configMetrics.stopTimer(sample);
        }
        
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
    
    private String getUserIdentity(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(decodedBytes);
                String[] parts = credentials.split(":", 2);
                return parts.length > 0 ? parts[0] : "anonymous";
            } catch (Exception e) {
                log.debug("Failed to extract user from Basic Auth header", e);
            }
        }
        
        String remoteUser = request.getRemoteUser();
        if (remoteUser != null && !remoteUser.isEmpty()) {
            return remoteUser;
        }
        
        return "anonymous";
    }
}
