package com.immortals.cache.providers.redis;

import com.immortals.cache.core.exception.CacheConfigurationException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "immortals.cache.redis")
public class RedisProperties {

    private String host;
    private Integer port;
    private String password;
    private Integer database;
    private Duration commandTimeout;
    private Duration timeToLive;
    private Boolean useSsl;
    private Boolean autoReconnect;
    private Integer poolMaxTotal;
    private Integer poolMaxIdle;
    private Integer poolMinIdle;
    private Duration poolMaxWait;
    private Boolean enable;
    private Boolean pingBeforeActivateConnection;
    private String namespace;
    private Cluster cluster = new Cluster();
    private Sentinel sentinel = new Sentinel();
    private Ssl ssl = new Ssl();
    private Acl acl = new Acl();
    private Pipelining pipelining = new Pipelining();
    private ReadStrategy readStrategy = new ReadStrategy();
    private Resilience resilience = new Resilience();

    @Getter
    @Setter
    public static class Cluster {
        private List<String> nodes = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Sentinel {
        private String master;
        private List<String> nodes = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Ssl {
        private String trustStore;
        private String trustStorePassword;
        private String keyStore;
        private String keyStorePassword;
    }

    @Getter
    @Setter
    public static class Acl {
        private Boolean enabled = false;
        private String username;
    }

    @Getter
    @Setter
    public static class Pipelining {
        private Boolean enabled = true;
        private Integer batchSize = 100;
    }

    @Getter
    @Setter
    public static class ReadStrategy {
        private Boolean readFromReplica = false;
        private String replicaPreference = "REPLICA_PREFERRED"; // REPLICA_PREFERRED, REPLICA, MASTER
    }

    @Getter
    @Setter
    public static class Resilience {
        private CircuitBreaker circuitBreaker = new CircuitBreaker();
        private StampedeProtection stampedeProtection = new StampedeProtection();
        private Timeout timeout = new Timeout();
    }

    @Getter
    @Setter
    public static class CircuitBreaker {
        private Boolean enabled = false;
        private Integer failureRateThreshold = 50;
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        private Integer slidingWindowSize = 100;
    }

    @Getter
    @Setter
    public static class StampedeProtection {
        private Boolean enabled = false;
        private Duration lockTimeout = Duration.ofSeconds(5);
    }

    @Getter
    @Setter
    public static class Timeout {
        private Boolean enabled = true;
        private Duration operationTimeout = Duration.ofSeconds(5);
    }

    public boolean isSslEnabled() {
        return Boolean.TRUE.equals(useSsl);
    }

    public boolean isAutoReconnectEnabled() {
        return Boolean.TRUE.equals(autoReconnect);
    }

    public boolean isPingBeforeActivateConnectionEnabled() {
        return Boolean.TRUE.equals(pingBeforeActivateConnection);
    }
    
    /**
     * Validates the Redis properties.
     * 
     * @throws CacheConfigurationException if validation fails
     */
    public void validate() {
        if (host == null || host.trim().isEmpty()) {
            throw new CacheConfigurationException(
                "Redis host must not be empty",
                "immortals.cache.redis.host",
                host
            );
        }
        
        if (port == null || port <= 0 || port > 65535) {
            throw new CacheConfigurationException(
                "Redis port must be between 1 and 65535",
                "immortals.cache.redis.port",
                port
            );
        }
    }
}
