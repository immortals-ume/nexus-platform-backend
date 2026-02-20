package com.immortals.authapp.service.impl;

import com.immortals.authapp.security.UserDetailsServiceImpl;
import com.immortals.authapp.security.jwt.JwtProvider;
import com.immortals.authapp.service.AuthService;
import com.immortals.authapp.service.LoginAttempt;
import com.immortals.authapp.service.TokenBlacklistService;
import com.immortals.platform.cache.providers.redis.RedisHashCacheService;
import com.immortals.platform.common.db.annotation.ReadOnly;
import com.immortals.platform.common.db.annotation.WriteOnly;
import com.immortals.platform.common.exception.AuthenticationException;
import com.immortals.platform.domain.auth.dto.LoginDto;
import com.immortals.platform.domain.auth.dto.LoginResponse;
import com.immortals.platform.domain.auth.event.UserAccountLockedEvent;
import com.immortals.platform.domain.auth.event.UserAuthenticatedEvent;
import com.immortals.platform.domain.auth.event.UserAuthenticationFailedEvent;
import com.immortals.platform.domain.auth.event.UserLoggedOutEvent;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.immortals.platform.domain.auth.constants.CacheConstants.REFRESH_TOKEN_HASH_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtProvider jwtProvider;
    private final RedisHashCacheService<String, String, Object> hashCacheService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CookieUtils cookieUtils;
    private final LoginAttempt loginAttempt;
    private final AuthEventPublisher authEventPublisher;

    @Value("${auth.refresh-token-expiry-ms}")
    private int refreshTokenExpiryMs;

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @WriteOnly
    @Override
    public LoginResponse login(LoginDto loginInfoDto) {
        String username = loginInfoDto.username();

        try {
            if (loginAttempt.isBlocked(username)) {
                Duration remainingTime = ((LoginAttemptService) loginAttempt).getRemainingBlockTime(username);
                log.warn("Login attempt blocked for user '{}', remaining block time: {}", username, remainingTime);
                publishAuthenticationFailed(username, "Account temporarily locked due to too many failed attempts");
                throw new AuthenticationException(
                        String.format("Your account is temporarily locked due to too many failed login attempts. Please try again in %d minutes.",
                                remainingTime.toMinutes()));
            }

            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginInfoDto.username(), loginInfoDto.password())
                );
            } catch (AuthenticationException e) {
                loginAttempt.loginFailed(username);

                if (loginAttempt.isBlocked(username)) {
                    publishUserAccountLocked(username);
                }

                publishAuthenticationFailed(username, "Invalid credentials");
                throw new AuthenticationException("Authentication failed. Please check your credentials.", e);
            }

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            if (!authentication.isAuthenticated()) {
                loginAttempt.loginFailed(username);
                publishAuthenticationFailed(username, "Authentication not successful");
                throw new AuthenticationException("Authentication failed. Please check your credentials.");
            }

            loginAttempt.loginSucceeded(username);

            String accessToken = jwtProvider.generateAccessToken(authentication);

            log.info("User '{}' successfully authenticated.", loginInfoDto.username());

            String refreshToken = "";
            if (loginInfoDto.rememberMe()) {
                refreshToken = jwtProvider.generateRefreshToken(authentication, refreshTokenExpiryMs);
                hashCacheService.put(
                        REFRESH_TOKEN_HASH_KEY,
                        loginInfoDto.username(),
                        refreshToken,
                        Duration.ofMillis(refreshTokenExpiryMs)
                );
                log.info("🔁 Refresh token generated and cached for user '{}'.", loginInfoDto.username());
            }

            publishAuthenticationSuccess(username, loginInfoDto.rememberMe());

            publishUserLoginEvent(username);

            return new LoginResponse(loginInfoDto.username(), accessToken, refreshToken, "Successfully generated the Login Token", );

        } catch (AuthenticationException e) {
            throw e;
        } catch (RuntimeException | IOException | NoSuchAlgorithmException | InvalidKeySpecException |
                 JOSEException e) {
            log.error("Login failed for user '{}': {}", loginInfoDto.username(), e.getMessage(), e);
            publishAuthenticationFailed(username, "System error during authentication");
            throw new AuthenticationException("Login failed. Please verify your credentials and try again.", e);
        }
    }

    @Cacheable(value = "refresh-tokens", key = "#username", condition = "#username != null", unless = "#result == null")
    @ReadOnly
    public String getCachedRefreshToken(String username) {
        Object token = hashCacheService.get(REFRESH_TOKEN_HASH_KEY, username);
        log.debug("Cache lookup for refresh token - username: {}, found: {}", username, token != null);
        return token != null ? token.toString() : null;
    }

    @CacheEvict(value = "refresh-tokens", key = "#username")
    @WriteOnly
    public void evictRefreshToken(String username) {
        hashCacheService.remove(REFRESH_TOKEN_HASH_KEY, username);
        log.debug("Evicted refresh token from cache for user: {}", username);
    }

    @Cacheable(value = "user-authentication", key = "#username", condition = "#username != null")
    @ReadOnly
    public boolean isUserAuthenticated(String username) {
        // This can be used to cache authentication status
        return userDetailsService.loadUserByUsername(username) != null;
    }

    @CacheEvict(value = "user-authentication", key = "#username")
    @WriteOnly
    public void evictUserAuthenticationCache(String username) {
        log.debug("Evicted user authentication cache for user: {}", username);
    }

    @Override
    @WriteOnly
    public LoginResponse generateRefreshToken(String username) {
        try {
            // Check cache first using declarative caching
            String existingToken = getCachedRefreshToken(username);
            if (existingToken != null) {
                log.info("Refresh token already exists for user '{}'.", username);
                return new LoginResponse(username, null, null, "Refresh token already exists.");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            String refreshToken = jwtProvider.generateRefreshToken(authentication, refreshTokenExpiryMs);

            // Store in hash cache from cache-starter
            hashCacheService.put(
                    REFRESH_TOKEN_HASH_KEY,
                    username,
                    refreshToken,
                    Duration.ofMillis(refreshTokenExpiryMs)
            );

            log.info("Refresh token generated for user '{}'.", username);
            return new LoginResponse(username, null, refreshToken, "Refresh token generated for user");
        } catch (Exception e) {
            log.error("Failed to generate refresh token for user '{}': {}", username, e.getMessage(), e);
            throw new AuthenticationException("Unable to generate refresh token. Please try again later.", e);
        }
    }

    @Override
    @WriteOnly
    public void logout(HttpServletRequest request) {
        try {
            Optional<String> token = cookieUtils.getRefreshTokenFromCookie(request);
            if (token.isPresent()) {
                String username = jwtProvider.getUsernameFromToken(token.get());

                // Blacklist the token
                tokenBlacklistService.blacklistToken(token.get(), refreshTokenExpiryMs);

                // Evict from cache using declarative caching
                evictRefreshToken(username);

                // Publish event for user service to handle logout status update
                publishUserLogoutEvent(username);

                log.info("Logout successful for user: {}", username);
            }
        } catch (RuntimeException | ParseException e) {
            log.error("Logout failed due to token parsing error.", e);
            throw new AuthenticationException("Logout failed. Unable to process token.");
        }
    }

    /**
     * Publish UserAuthenticated event
     */
    private void publishAuthenticationSuccess(String username, boolean rememberMe) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            UserAuthenticatedEvent event = UserAuthenticatedEvent.builder()
                    .username(username)
                    .authenticatedAt(Instant.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .rememberMe(rememberMe)
                    .build();

            // Use username as userId for now (user service will handle user ID resolution)
            authEventPublisher.publishUserAuthenticated(event, username);
        } catch (Exception e) {
            log.error("Failed to publish authentication success event for user: {}", username, e);
        }
    }

    /**
     * Publish UserAuthenticationFailed event
     */
    private void publishAuthenticationFailed(String username, String reason) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            UserAuthenticationFailedEvent event = UserAuthenticationFailedEvent.builder()
                    .username(username)
                    .failedAt(Instant.now())
                    .reason(reason)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            authEventPublisher.publishUserAuthenticationFailed(event);
        } catch (Exception e) {
            log.error("Failed to publish authentication failed event for user: {}", username, e);
        }
    }

    /**
     * Publish UserAccountLocked event
     */
    private void publishUserAccountLocked(String username) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            int failedAttempts = loginAttempt.getFailedAttempts(username);
            Duration lockDuration = ((LoginAttemptService) loginAttempt).getRemainingBlockTime(username);

            UserAccountLockedEvent event = UserAccountLockedEvent.builder()
                    .username(username)
                    .lockedAt(Instant.now())
                    .failedAttempts(failedAttempts)
                    .lockDuration(lockDuration)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .reason("Too many failed login attempts")
                    .build();

            authEventPublisher.publishUserAccountLocked(event);
        } catch (Exception e) {
            log.error("Failed to publish user account locked event for user: {}", username, e);
        }
    }

    /**
     * Publish user login event for user service to handle
     */
    private void publishUserLoginEvent(String username) {
        try {
            // This will be handled by user service via event listening
            log.debug("User login event will be handled by user service for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to publish user login event for user: {}", username, e);
        }
    }

    /**
     * Publish user logout event for user service to handle
     */
    private void publishUserLogoutEvent(String username) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            UserLoggedOutEvent event = UserLoggedOutEvent.builder()
                    .username(username)
                    .loggedOutAt(Instant.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .sessionId(null) // Could be extracted from request if needed
                    .forced(false)
                    .build();

            authEventPublisher.publishUserLoggedOut(event);
            log.debug("Published user logout event for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to publish user logout event for user: {}", username, e);
        }
    }

    /**
     * Get current HTTP request from RequestContextHolder
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("Could not retrieve current request", e);
            return null;
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
