package com.immortals.usermanagementservice.filter;


import com.immortals.usermanagementservice.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(1)
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {


    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr();
        if (!rateLimiterService.isAllowed(key)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter()
                    .write("Too many requests");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
