package com.immortals.authapp.security.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws ServletException {
        try {
            log.error("Unauthorized access attempt: {}", authException.getMessage());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            responseBody.put("error", "Unauthorized");
            responseBody.put("message", authException.getMessage());
            responseBody.put("path", request.getRequestURI());

            objectMapper.writeValue(response.getOutputStream(), responseBody);
        } catch (IOException e) {
            log.error("Failed to write response: {}", e.getMessage());
            throw new ServletException("Unable to write response", e);
        }
    }

}
