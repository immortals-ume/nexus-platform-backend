package com.immortals.platform.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.cache.annotation.Cacheable;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Super-efficient JWT utility combining generation and validation.
 * 
 * Features:
 * - RSA256 signing and verification
 * - Token generation (requires private key)
 * - Token validation (requires public key)
 * - Claims extraction
 * - Caching for validation results
 * - Thread-safe operations
 * 
 * Usage:
 * - auth-app: Uses both generation and validation (has private + public key)
 * - Other services: Uses only validation (has public key only)
 */
@Slf4j
public class JwtUtils {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String issuer;
    private final boolean cacheEnabled;

    /**
     * Constructor for services that only validate tokens (resource servers).
     * 
     * @param publicKey RSA public key for validation
     * @param issuer Expected token issuer
     * @param cacheEnabled Enable caching of validation results
     */
    public JwtUtils(RSAPublicKey publicKey, String issuer, boolean cacheEnabled) {
        this.privateKey = null;
        this.publicKey = publicKey;
        this.issuer = issuer;
        this.cacheEnabled = cacheEnabled;
        log.info("JWT Utils initialized for validation only (resource server mode)");
    }

    /**
     * Constructor for auth service that generates and validates tokens.
     * 
     * @param privateKey RSA private key for signing
     * @param publicKey RSA public key for validation
     * @param issuer Token issuer
     * @param cacheEnabled Enable caching of validation results
     */
    public JwtUtils(RSAPrivateKey privateKey, RSAPublicKey publicKey, String issuer, boolean cacheEnabled) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.issuer = issuer;
        this.cacheEnabled = cacheEnabled;
        log.info("JWT Utils initialized for generation and validation (auth server mode)");
    }

    // ==================== KEY LOADING ====================

    /**
     * Load RSA private key from PEM string.
     */
    public static RSAPrivateKey loadPrivateKeyFromPem(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        log.debug("Loading RSA private key from PEM");
        String key = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.decodeBase64(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    /**
     * Load RSA public key from PEM string.
     */
    public static RSAPublicKey loadPublicKeyFromPem(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        log.debug("Loading RSA public key from PEM");
        String key = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.decodeBase64(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    // ==================== TOKEN GENERATION ====================

    /**
     * Generate JWT token with custom claims.
     * Requires private key (auth service only).
     * 
     * @param subject Token subject (username)
     * @param claims Custom claims to include
     * @param expiryDuration Token expiry duration
     * @return Signed JWT token string
     */
    public String generateToken(String subject, JWTClaimsSet.Builder claims, Duration expiryDuration) throws JOSEException {
        if (privateKey == null) {
            throw new IllegalStateException("Private key not available. Token generation requires auth server mode.");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiryDuration);

        JWTClaimsSet claimsSet = claims
                .subject(subject)
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(issuedAt))
                .issuer(issuer)
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
        RSASSASigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);

        log.debug("Token generated for subject: {} (exp: {})", subject, expiresAt);
        return signedJWT.serialize();
    }

    /**
     * Generate access token with user details.
     */
    public String generateAccessToken(String username, Long userId, String email, 
                                     List<String> roles, List<String> permissions, 
                                     Duration expiryDuration) throws JOSEException {
        log.info("Generating access token for user: {}", username);
        
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("type", "access");

        return generateToken(username, claims, expiryDuration);
    }

    /**
     * Generate refresh token.
     */
    public String generateRefreshToken(String username, Long userId, Duration expiryDuration) throws JOSEException {
        log.info("Generating refresh token for user: {}", username);
        
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .claim("userId", userId)
                .claim("type", "refresh");

        return generateToken(username, claims, expiryDuration);
    }

    /**
     * Generate guest token.
     */
    public String generateGuestToken(Duration expiryDuration) throws JOSEException {
        String guestId = "guest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        log.info("Generating guest token for subject: {}", guestId);
        
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .claim("roles", List.of("ROLE_GUEST"))
                .claim("type", "guest");

        return generateToken(guestId, claims, expiryDuration);
    }

    // ==================== TOKEN VALIDATION ====================

    /**
     * Validate JWT token signature and expiration.
     * Results are cached if caching is enabled.
     * 
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    @Cacheable(value = "jwtValidation", key = "#token", unless = "#result == false", condition = "#root.target.cacheEnabled")
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            
            // Verify signature
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return false;
            }
            
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            // Verify expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || new Date().after(expirationTime)) {
                log.warn("JWT token expired");
                return false;
            }
            
            // Verify issuer
            if (issuer != null && !issuer.equals(claims.getIssuer())) {
                log.warn("JWT issuer mismatch. Expected: {}, Got: {}", issuer, claims.getIssuer());
                return false;
            }
            
            log.debug("JWT token validated successfully for subject: {}", claims.getSubject());
            return true;
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate token and return claims if valid.
     * More efficient than separate validate + getClaims calls.
     * 
     * @param token JWT token string
     * @return JWTClaimsSet if valid, null if invalid
     */
    public JWTClaimsSet validateAndGetClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            
            if (!signedJWT.verify(verifier)) {
                return null;
            }
            
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            // Check expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || new Date().after(expirationTime)) {
                return null;
            }
            
            // Check issuer
            if (issuer != null && !issuer.equals(claims.getIssuer())) {
                return null;
            }
            
            return claims;
        } catch (Exception e) {
            log.error("Token validation and claims extraction failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== CLAIMS EXTRACTION ====================

    /**
     * Extract username/subject from token.
     */
    public String getUsernameFromToken(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        String subject = signedJWT.getJWTClaimsSet().getSubject();
        log.debug("Extracted subject from token: {}", subject);
        return subject;
    }

    /**
     * Extract all claims from token.
     */
    public JWTClaimsSet getClaimsFromToken(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        log.debug("Extracted claims from token for subject: {}", claims.getSubject());
        return claims;
    }

    /**
     * Extract user ID from token.
     */
    public Long getUserIdFromToken(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        Object userId = claims.getClaim("userId");
        return userId != null ? ((Number) userId).longValue() : null;
    }

    /**
     * Extract email from token.
     */
    public String getEmailFromToken(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        return (String) claims.getClaim("email");
    }

    /**
     * Extract roles from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        List<String> roles = (List<String>) claims.getClaim("roles");
        log.debug("Extracted roles from token: {}", roles);
        return roles != null ? roles : List.of();
    }

    /**
     * Extract permissions from token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        List<String> permissions = (List<String>) claims.getClaim("permissions");
        log.debug("Extracted permissions from token: {}", permissions);
        return permissions != null ? permissions : List.of();
    }

    /**
     * Get remaining TTL for token.
     */
    public Duration getExpiryTimeFromToken(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        Date exp = claims.getExpirationTime();
        long ttl = exp.getTime() - System.currentTimeMillis();
        log.debug("Token TTL (ms): {}", Math.max(ttl, 0));
        return Duration.ofMillis(Math.max(ttl, 0));
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            JWTClaimsSet claims = getClaimsFromToken(token);
            Date expirationTime = claims.getExpirationTime();
            return expirationTime != null && new Date().after(expirationTime);
        } catch (ParseException e) {
            return true;
        }
    }

    /**
     * Get token type (access, refresh, guest).
     */
    public String getTokenType(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        return (String) claims.getClaim("type");
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if this instance can generate tokens.
     */
    public boolean canGenerateTokens() {
        return privateKey != null;
    }

    /**
     * Get issuer.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Check if caching is enabled.
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
}
