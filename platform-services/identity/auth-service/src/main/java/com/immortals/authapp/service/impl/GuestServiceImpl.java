package com.immortals.authapp.service.impl;

import com.immortals.authapp.security.jwt.JwtProvider;
import com.immortals.authapp.service.GuestService;
import com.immortals.platform.common.exception.TechnicalException;
import com.immortals.platform.common.db.annotation.ReadOnly;
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
    @ReadOnly
    public String generateGuestLogin() {
        try {
            String token = jwtProvider.generateGuestToken(Duration.ofMillis(jwtExpirationMs));
            log.info("Successfully generated guest JWT token with expiry {} ms.", jwtExpirationMs);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate guest JWT token: {}", e.getMessage(), e);
            throw new TechnicalException("Unable to generate guest token at this time.", e);
        }
    }
}
