package com.immortals.platform.security.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;

/**
 * JWT token validator for resource servers.
 * Validates JWT tokens using RSA public key verification.
 * This is a wrapper around JwtUtils for backward compatibility.
 * 
 * @deprecated Use JwtUtils directly for better performance
 */
@Slf4j
@Deprecated
public class JwtTokenValidator {

    private final JwtUtils jwtUtils;

    public JwtTokenValidator(RSAPublicKey publicKey, String issuer) {
        this.jwtUtils = new JwtUtils(publicKey, issuer, true);
        log.info("JWT Token Validator initialized with issuer: {}", issuer);
    }

    public JwtTokenValidator(RSAPublicKey publicKey, String issuer, boolean cacheEnabled) {
        this.jwtUtils = new JwtUtils(publicKey, issuer, cacheEnabled);
        log.info("JWT Token Validator initialized with issuer: {} (cache: {})", issuer, cacheEnabled);
    }

    // Delegate all methods to JwtUtils

    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    public String getUsernameFromToken(String token) throws ParseException {
        return jwtUtils.getUsernameFromToken(token);
    }

    public JWTClaimsSet getClaimsFromToken(String token) throws ParseException {
        return jwtUtils.getClaimsFromToken(token);
    }

    public List<String> getRolesFromToken(String token) throws ParseException {
        return jwtUtils.getRolesFromToken(token);
    }

    public List<String> getPermissionsFromToken(String token) throws ParseException {
        return jwtUtils.getPermissionsFromToken(token);
    }

    public Duration getExpiryTimeFromToken(String token) throws ParseException {
        return jwtUtils.getExpiryTimeFromToken(token);
    }

    public Long getUserIdFromToken(String token) throws ParseException {
        return jwtUtils.getUserIdFromToken(token);
    }

    public String getEmailFromToken(String token) throws ParseException {
        return jwtUtils.getEmailFromToken(token);
    }

    public JWTClaimsSet validateAndGetClaims(String token) {
        return jwtUtils.validateAndGetClaims(token);
    }
}
