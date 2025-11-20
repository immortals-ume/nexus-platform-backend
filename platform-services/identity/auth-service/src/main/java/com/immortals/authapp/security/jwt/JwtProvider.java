package com.immortals.authapp.security.jwt;

import com.immortals.platform.domain.helper.UserPrincipal;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

@Component
@Slf4j
public class JwtProvider {

    @Value("${jwt-private-key}")
    private String privateKeyPem;

    @Value("${jwt-public-key}")
    private String publicKeyPem;

    @Getter
    @Value("${auth.access-token-expiry-ms}")
    private int jwtExpirationMs;

    @Getter
    @Value("${auth.jwt-issuer}")
    private String jwtIssuer;

    @Getter
    private RSAPrivateKey privateKey;
    @Getter
    private RSAPublicKey publicKey;

    @PostConstruct
    public void initKeys() throws Exception {
        log.info("Initializing JWT keys...");
        this.privateKey = loadPrivateKeyFromPem(privateKeyPem);
        this.publicKey = loadPublicKeyFromPem(publicKeyPem);
        log.info("JWT keys successfully initialized.");
    }

    private RSAPrivateKey loadPrivateKeyFromPem(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        log.debug("Loading RSA private key from PEM.");
        String key = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.decodeBase64(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKeyFromPem(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        log.debug("Loading RSA public key from PEM.");
        String key = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.decodeBase64(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    public String generateAccessToken(Authentication authentication) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        log.info("Generating access token for user: {}", authentication.getName());
        return getToken(authentication, jwtExpirationMs);
    }

    public String generateRefreshToken(Authentication authentication, Integer refreshExpiryTime) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
        log.info("Generating refresh token for user: {}", authentication.getName());
        return getToken(authentication, refreshExpiryTime);
    }

    private String getToken(Authentication authentication, Integer expiryTime) throws  JOSEException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expiryTime);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userPrincipal.getUsername())
                .claim("userId", userPrincipal.getUserId())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", userPrincipal.getAuthorities())
                .claim("permissions", userPrincipal.getPermissions())
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(issuedAt))
                .issuer(jwtIssuer)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);

        RSASSASigner signer = new RSASSASigner(getPrivateKey());
        signedJWT.sign(signer);

        log.debug("Token generated for user: {} (exp: {})", userPrincipal.getUsername(), expiresAt);
        return signedJWT.serialize();
    }


    public String generateGuestToken(Duration expiry) throws JOSEException {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiry);

        String guestId = "guest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(guestId)
                .claim("roles", List.of("ROLE_GUEST"))
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(issuedAt))
                .issuer(jwtIssuer)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        RSASSASigner signer = new RSASSASigner(getPrivateKey());
        signedJWT.sign(signer);

        log.info("Guest token generated for subject: {} (exp: {})", guestId, expiresAt);
        return signedJWT.serialize();
    }


    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSASSAVerifier verifier = new RSASSAVerifier(getPublicKey());
            boolean valid = signedJWT.verify(verifier) && new Date().before(signedJWT.getJWTClaimsSet().getExpirationTime());
            log.debug("Token validation result: {}", valid);
            return valid;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            return Boolean.FALSE;
        }
    }

    public String getUsernameFromToken(String token) throws ParseException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String subject = signedJWT.getJWTClaimsSet().getSubject();
            log.debug("Extracted subject from token: {}", subject);
            return subject;
        } catch (ParseException e) {
            log.error("Failed to extract username from token: {}", e.getMessage(), e);
            throw e;
        }
    }

    public JWTClaimsSet getClaimsFromToken(String token) throws ParseException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            log.debug("Extracted claims from token for subject: {}", claims.getSubject());
            return claims;
        } catch (ParseException e) {
            log.error("Invalid JWT token format: {}", e.getMessage(), e);
            throw new ParseException("Invalid JWT token format: " + e.getMessage(), e.getErrorOffset());
        }
    }

    public List<String> getPermissionsFromToken(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        List<String> permissions = claims.getClaim("permissions") != null ? (List<String>) claims.getClaim("permissions") : List.of();
        log.debug("Extracted permissions from token: {}", permissions);
        return permissions;
    }

    public Duration getExpiryTimeFromToken(String token) throws ParseException {
        JWTClaimsSet claims = getClaimsFromToken(token);
        Date exp = claims.getExpirationTime();
        long ttl = exp.getTime() - System.currentTimeMillis();
        log.debug("Token TTL (ms): {}", Math.max(ttl, 0));
        return Duration.ofMillis(Math.max(ttl, 0));
    }
}
