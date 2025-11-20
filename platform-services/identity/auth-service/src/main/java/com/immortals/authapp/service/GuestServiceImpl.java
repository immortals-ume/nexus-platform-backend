package com.immortals.authapp.service;

import com.immortals.authapp.security.jwt.JwtProvider;
import com.immortals.authapp.service.exception.GuestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestServiceImpl implements GuestService {

    private final JwtProvider jwtProvider;

    @Value("${auth.guest-token-expiry-ms}")
    private int jwtExpirationMs;

    @Override
    public String generateGuestLogin() {
        try {
            String token = jwtProvider.generateGuestToken(Duration.ofMillis(jwtExpirationMs));
            log.info("Successfully generated guest JWT token with expiry {} ms.", jwtExpirationMs);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate guest JWT token: {}", e.getMessage(), e);
            throw new GuestException("Unable to generate guest token at this time.", e);
        }
    }
}
