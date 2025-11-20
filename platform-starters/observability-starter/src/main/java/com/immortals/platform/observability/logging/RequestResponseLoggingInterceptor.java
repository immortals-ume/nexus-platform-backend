package com.immortals.platform.observability.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for logging HTTP requests and responses.
 * Logs request details including method, URI, and response status.
 */
@Slf4j
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        
        log.info("Incoming request: method={}, uri={}, remoteAddr={}", 
                request.getMethod(), 
                request.getRequestURI(), 
                request.getRemoteAddr());
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - (startTime != null ? startTime : 0);
        
        if (ex != null) {
            log.error("Request completed with exception: method={}, uri={}, status={}, duration={}ms, exception={}", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    response.getStatus(), 
                    duration,
                    ex.getMessage());
        } else {
            log.info("Request completed: method={}, uri={}, status={}, duration={}ms", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    response.getStatus(), 
                    duration);
        }
    }
}
