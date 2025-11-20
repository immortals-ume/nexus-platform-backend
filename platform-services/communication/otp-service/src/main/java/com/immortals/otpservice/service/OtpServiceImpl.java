package com.immortals.otpservice.service;

import com.immortals.cacheservice.service.MultiLevelCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;

/**
 * OTP Service implementation using platform cache-starter.
 * Uses MultiLevelCacheService for L1 (Caffeine) + L2 (Redis) caching.
 * 
 * Benefits:
 * - L1 cache: Ultra-fast local access (<1ms) for repeated verifications
 * - L2 cache: Distributed Redis for cross-instance sharing
 * - Automatic cache invalidation across all instances
 * - Circuit breaker protection if Redis fails
 * - Comprehensive metrics and monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_TTL = Duration.ofHours(1);
    private static final int MAX_ATTEMPTS_PER_HOUR = 3;

    private final MultiLevelCacheService cacheService; // L1 (Caffeine) + L2 (Redis)
    private final Random random = new Random();

    private final Random random = new Random();

    @Override
    public void sendOtp(String mobile) {
        log.info("Generating OTP for mobile: {}", mobile);

        String otpKey = OTP_KEY_PREFIX + mobile;
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + mobile;

        // Check rate limit
        checkRateLimit(rateLimitKey);

        // Generate OTP with stampede protection (prevents duplicate generation)
        // Uses distributed lock to ensure only one thread generates OTP
        String otp = cacheService.getOrCompute(
                otpKey,
                String.class,
                this::generateOtp,
                OTP_TTL
        ).orElseThrow(() -> new RuntimeException("Failed to generate OTP"));

        // Increment rate limit counter
        incrementRateLimitCounter(rateLimitKey);

        log.info("OTP sent successfully for mobile: {} (cached in L1+L2)", mobile);
        // In real implementation, send SMS here via SMS gateway
    }

    @Override
    public void verifyOtp(String mobile, String otp) {
        log.info("Verifying OTP for mobile: {}", mobile);

        String otpKey = OTP_KEY_PREFIX + mobile;

        // Check L1 cache first (Caffeine - ultra fast <1ms)
        // If L1 miss, check L2 cache (Redis - fast ~2ms)
        // Metrics automatically track L1 vs L2 hits
        Optional<String> cachedOtp = cacheService.get(otpKey, String.class);

        if (cachedOtp.isEmpty()) {
            log.warn("OTP not found or expired for mobile: {}", mobile);
            throw new RuntimeException("OTP expired or not found");
        }

        if (!cachedOtp.get().equals(otp)) {
            log.warn("Invalid OTP provided for mobile: {}", mobile);
            throw new RuntimeException("Invalid OTP");
        }

        // Remove OTP from both L1 and L2 after successful verification
        cacheService.remove(otpKey);
        log.info("OTP verified successfully for mobile: {} (removed from L1+L2)", mobile);
    }

    @Override
    public void resendOtp(String mobile) {
        log.info("Resending OTP for mobile: {}", mobile);

        String otpKey = OTP_KEY_PREFIX + mobile;
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + mobile;

        // Check rate limit
        checkRateLimit(rateLimitKey);

        // Remove existing OTP from both L1 and L2
        cacheService.remove(otpKey);

        // Generate new OTP
        sendOtp(mobile);
    }

    @Override
    public boolean isOtpExpired(String mobile) {
        String otpKey = OTP_KEY_PREFIX + mobile;
        Optional<String> cachedOtp = cacheService.get(otpKey, String.class);
        boolean expired = cachedOtp.isEmpty();
        log.debug("OTP expired check for mobile {}: {}", mobile, expired);
        return expired;
    }

    /**
     * Generate a random 6-digit OTP.
     */
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(otp);
        log.debug("Generated new OTP: {}", otpCode);
        return otpCode;
    }

    /**
     * Check if rate limit is exceeded.
     */
    private void checkRateLimit(String rateLimitKey) {
        Optional<Integer> attempts = cacheService.get(rateLimitKey, Integer.class);

        if (attempts.isPresent() && attempts.get() >= MAX_ATTEMPTS_PER_HOUR) {
            log.warn("Rate limit exceeded for key: {}", rateLimitKey);
            throw new RuntimeException("Rate limit exceeded. Maximum " + MAX_ATTEMPTS_PER_HOUR + " OTPs per hour.");
        }
    }

    /**
     * Increment rate limit counter in cache.
     */
    private void incrementRateLimitCounter(String rateLimitKey) {
        Optional<Integer> attempts = cacheService.get(rateLimitKey, Integer.class);
        int newAttempts = attempts.orElse(0) + 1;
        cacheService.put(rateLimitKey, newAttempts, RATE_LIMIT_TTL);
        log.debug("Rate limit counter for key {}: {}/{}", rateLimitKey, newAttempts, MAX_ATTEMPTS_PER_HOUR);
    }
}
