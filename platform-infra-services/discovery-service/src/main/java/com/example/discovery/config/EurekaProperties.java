package com.example.discovery.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Eureka Discovery Service.
 * Validates required properties at startup to ensure proper configuration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "eureka")
@Validated
public class EurekaProperties {
    
    @Valid
    @NotNull(message = "Eureka instance configuration is required")
    private Instance instance = new Instance();
    
    @Valid
    @NotNull(message = "Eureka server configuration is required")
    private Server server = new Server();
    
    @Valid
    @NotNull(message = "Eureka client configuration is required")
    private Client client = new Client();
    
    @Data
    @Validated
    public static class Instance {
        @NotBlank(message = "Eureka hostname is required")
        private String hostname = "localhost";
        
        private boolean preferIpAddress = false;
        
        @Min(value = 1, message = "Lease renewal interval must be at least 1 second")
        @Max(value = 300, message = "Lease renewal interval must not exceed 300 seconds")
        private int leaseRenewalIntervalInSeconds = 30;
        
        @Min(value = 1, message = "Lease expiration duration must be at least 1 second")
        @Max(value = 600, message = "Lease expiration duration must not exceed 600 seconds")
        private int leaseExpirationDurationInSeconds = 90;
    }
    
    @Data
    @Validated
    public static class Server {
        private boolean enableSelfPreservation = true;
        
        @Min(value = 0, message = "Renewal percent threshold must be between 0 and 1")
        @Max(value = 1, message = "Renewal percent threshold must be between 0 and 1")
        private double renewalPercentThreshold = 0.85;
        
        @Min(value = 1000, message = "Eviction interval must be at least 1000ms")
        private int evictionIntervalTimerInMs = 30000;
        
        @Min(value = 1000, message = "Response cache update interval must be at least 1000ms")
        private int responseCacheUpdateIntervalMs = 30000;
        
        @Min(value = 1, message = "Response cache auto expiration must be at least 1 second")
        private int responseCacheAutoExpirationInSeconds = 180;
    }
    
    @Data
    @Validated
    public static class Client {
        private boolean registerWithEureka = false;
        private boolean fetchRegistry = false;
        
        @Valid
        @NotNull(message = "Service URL configuration is required")
        private ServiceUrl serviceUrl = new ServiceUrl();
        
        @Data
        @Validated
        public static class ServiceUrl {
            @NotBlank(message = "Default zone URL is required")
            private String defaultZone = "http://localhost:8761/eureka/";
        }
    }
}
