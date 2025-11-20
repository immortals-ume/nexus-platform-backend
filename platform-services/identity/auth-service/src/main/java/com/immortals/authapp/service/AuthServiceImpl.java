package com.immortals.authapp.service;

import com.immortals.authapp.event.UserAuthenticatedEvent;
import com.immortals.authapp.event.UserAuthenticationFailedEvent;
import com.immortals.platform.domain.dto.LoginDto;
import com.immortals.platform.domain.dto.LoginResponse;
import com.immortals.authapp.security.UserDetailsServiceImpl;
import com.immortals.authapp.security.jwt.JwtProvider;
import com.immortals.authapp.service.cache.CacheService;
import com.immortals.authapp.service.user.UserService;
import com.immortals.platform.common.exception.BusinessException;
import com.immortals.authapp.utils.CookieUtils;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
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

import static com.immortals.authapp.constants.CacheConstants.REFRESH_TOKEN_HASH_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtProvider jwtProvider;
    private final CacheService<String, String, String> hashCacheService;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CookieUtils cookieUtils;
    private final LoginAttempt loginAttempt;
    private final AuthEventPublisher authEventPublisher;

    @Value("${auth.refresh-token-expiry-ms}")
    private int refreshTokenExpiryMs;


    @Transactional
    @Override
    public LoginResponse login(LoginDto loginInfoDto) {
        String username = loginInfoDto.username();
        
        try {
            if (loginAttempt.isBlocked(username)) {
                log.warn("Login attempt blocked for user '{}'", username);
                publishAuthenticationFailed(username, "Account temporarily locked due to too many failed attempts");
                throw new AuthenticationException("Your account is temporarily locked due to too many failed login attempts. Please try again later.");
            }

            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginInfoDto.username(), loginInfoDto.password())
                );
            } catch (AuthenticationException e) {
                loginAttempt.loginFailed(username);
                publishAuthenticationFailed(username, "Invalid credentials");
                throw new AuthenticationException("Authentication failed. Please check your credentials.", e);
            }
            
            SecurityContextHolder.getContext().setAuthentication(authentication);

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
                        Duration.ofMillis(refreshTokenExpiryMs),
                        loginInfoDto.username()
                );
                log.info("🔁 Refresh token generated and cached for user '{}'.", loginInfoDto.username());
            }

            publishAuthenticationSuccess(username, loginInfoDto.rememberMe());

            userService.updateLoginStatus(loginInfoDto.username()); //async
            return new LoginResponse(loginInfoDto.username(), accessToken, refreshToken,"Successfully generated the Login Token");

        } catch (AuthenticationException e) {
            throw e;
        } catch (RuntimeException | IOException | NoSuchAlgorithmException | InvalidKeySpecException |
                 JOSEException e) {
            log.error("Login failed for user '{}': {}", loginInfoDto.username(), e.getMessage(), e);
            publishAuthenticationFailed(username, "System error during authentication");
            throw new AuthenticationException("Login failed. Please verify your credentials and try again.", e);
        }
    }

    @Override
    public LoginResponse generateRefreshToken(String username) {
        try {
            if (hashCacheService.containsKey(REFRESH_TOKEN_HASH_KEY, username, username)) {
                log.info("Refresh token already exists for user '{}'.", username);
                return new LoginResponse(username,null,null,"Refresh token already exists.");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            String refreshToken = jwtProvider.generateRefreshToken(authentication, refreshTokenExpiryMs);
            hashCacheService.put(
                    REFRESH_TOKEN_HASH_KEY,
                    username,
                    refreshToken,
                    Duration.ofMillis(refreshTokenExpiryMs),
                    username
            );

            log.info("Refresh token generated for user '{}'.", username);
            return new LoginResponse(username,null,refreshToken,"Refresh token generated for user");
        } catch (Exception e) {
            log.error("Failed to generate refresh token for user '{}': {}", username, e.getMessage(), e);
            throw new AuthenticationException("Unable to generate refresh token. Please try again later.", e);
        }
    }

    @Override
    public void logout(HttpServletRequest request) {
        try {

            Optional<String> token = cookieUtils.getRefreshTokenFromCookie(request) ;
            if (token.isPresent()) {
                tokenBlacklistService.blacklistToken(token.get(), refreshTokenExpiryMs);
                userService.updateLogoutStatus(jwtProvider.getUsernameFromToken(token.get()));
                log.info("Logout successful");
            }

        } catch (RuntimeException | ParseException e) {
            log.error(" Logout failed due to token parsing error.", e);
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

            // Get user ID for the event
            String userId = userService.getUserByUsername(username).getUserId().toString();
            authEventPublisher.publishUserAuthenticated(event, userId);
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
