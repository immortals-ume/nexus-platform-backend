package com.immortals.otpservice.service;

import com.immortals.cacheservice.client.CacheClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

/**
 * OTP Service implementation using CacheClient (Strategy Pattern).
 * 
 * This is the RECOMMENDED approach - uses CacheClient which automatically
 * selects the right caching strategy based on configuration.
 * 
 * Benefits:
 * - Simple API - just inject CacheClient
 * - Configuration-driven - change strategy via YAML
 * - No need to know about internal implementations
 * - All features available automatically
 * 
 * @since 2.0.0
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class OtpServiceWithCacheClient implements OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_TTL = Duration.ofHours(1);
    private static final int MAX_ATTEMPTS_PER_HOUR = 3;

    private final CacheClient cache; // Simple! Just one dependency
    private final Random random = new Random();

    @Override
    public void sendOtp(String mobile) {
        log.info("Generating OTP for mobile: {} using strategy: {}", mobile, cache.getStrategyName());

        String otpKey = "otp:" + mobile;
        String rateLimitKey = "rate:" + mobile;

        // Check rate limit
        checkRateLimit(rateLimitKey);

        // Generate OTP with stampede protection
        // CacheClient automatically uses the configured strategy
        String otp = cache.getOrCompute(
                otpKey,
                String.class,
                this::generateOtp,
                OTP_TTL
        ).orElseThrow(() -> new RuntimeException("Failed to generate OTP"));

        // Increment rate limit counter
        incrementRateLimitCounter(rateLimitKey);

        log.info("OTP sent successfully for mobile: {}", mobile);
    }

    @Override
    public void verifyOtp(String mobile, String otp) {
        log.info("Verifying OTP for mobile: {}", mobile);

        String otpKey = "otp:" + mobile;

        // Get from cache - strategy handles L1/L2/Redis automatically
        Optional<String> cachedOtp = cache.get(otpKey, String.class);

        if (cachedOtp.isEmpty()) {
            log.warn("OTP not found or expired for mobile: {}", mobile);
            throw new RuntimeException("OTP expired or not found");
        }

        if (!cachedOtp.get().equals(otp)) {
            log.warn("Invalid OTP provided for mobile: {}", mobile);
            throw new RuntimeException("Invalid OTP");
        }

        // Remove OTP after successful verification
        cache.remove(otpKey);
        log.info("OTP verified successfully for mobile: {}", mobile);
    }

    @Override
    public void resendOtp(String mobile) {
        log.info("Resending OTP for mobile: {}", mobile);

        String otpKey = "otp:" + mobile;
        String rateLimitKey = "rate:" + mobile;

        // Check rate limit
        checkRateLimit(rateLimitKey);

        // Remove existing OTP
        cache.remove(otpKey);

        // Generate new OTP
        sendOtp(mobile);
    }

    @Override
    public boolean isOtpExpired(String mobile) {
        String otpKey = "otp:" + mobile;
        Optional<String> cachedOtp = cache.get(otpKey, String.class);
        boolean expired = cachedOtp.isEmpty();
        log.debug("OTP expired check for mobile {}: {}", mobile, expired);
        return expired;
    }

    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(otp);
        log.debug("Generated new OTP: {}", otpCode);
        return otpCode;
    }

    private void checkRateLimit(String rateLimitKey) {
        Optional<Integer> attempts = cache.get(rateLimitKey, Integer.class);

        if (attempts.isPresent() && attempts.get() >= MAX_ATTEMPTS_PER_HOUR) {
            log.warn("Rate limit exceeded for key: {}", rateLimitKey);
            throw new RuntimeException("Rate limit exceeded. Maximum " + MAX_ATTEMPTS_PER_HOUR + " OTPs per hour.");
        }
    }

    private void incrementRateLimitCounter(String rateLimitKey) {
        Optional<Integer> attempts = cache.get(rateLimitKey, Integer.class);
        int newAttempts = attempts.orElse(0) + 1;
        cache.put(rateLimitKey, newAttempts, RATE_LIMIT_TTL);
        log.debug("Rate limit counter for key {}: {}/{}", rateLimitKey, newAttempts, MAX_ATTEMPTS_PER_HOUR);
    }
}
