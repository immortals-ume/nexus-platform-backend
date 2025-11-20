package com.immortals.platform.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for security-starter.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private RateLimit rateLimit = new RateLimit();
    private Cors cors = new Cors();
    private Audit audit = new Audit();

    @Getter
    @Setter
    public static class Jwt {
        private Boolean enabled = true;
        private String publicKey;
        private String issuer;
        private String jwkSetUri;
        private Boolean cacheEnabled = true;
        private Long cacheTtlSeconds = 300L;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private Boolean enabled = true;
        private Integer defaultLimit = 100; // requests per minute
        private Integer timeWindowSeconds = 60;
        private Boolean ipBased = true;
        private Boolean userBased = true;
        private List<String> excludedPaths = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Cors {
        private Boolean enabled = true;
        private List<String> allowedOrigins = List.of("*");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private Boolean allowCredentials = true;
        private Long maxAge = 3600L;
    }

    @Getter
    @Setter
    public static class Audit {
        private Boolean enabled = true;
        private Boolean logAuthenticationAttempts = true;
        private Boolean logAuthorizationFailures = true;
        private Boolean logRateLimitViolations = true;
    }
}
