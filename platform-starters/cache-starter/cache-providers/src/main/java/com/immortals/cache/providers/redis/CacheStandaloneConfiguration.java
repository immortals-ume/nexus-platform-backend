package com.immortals.cache.providers.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SslOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;
import com.immortals.cache.core.exception.CacheConnectionException;
import com.immortals.cache.providers.resilience.DecoratorChainFactory;
import org.redisson.api.RedissonClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;

@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
@ConditionalOnProperty(name = "cache.redis.enable", havingValue = "true", matchIfMissing = true)
public class CacheStandaloneConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CacheStandaloneConfiguration.class);
    private static final int SSL_RETRY_ATTEMPTS = 3;

    /**
     * Creates RedisProperties bean only when not using the cache starter.
     * When using cache starter, RedisProperties is created by MultiLevelAutoConfiguration.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "immortals.cache.type", havingValue = "", matchIfMissing = true)
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties props) {
        try {
            // Validate properties first
            validateRedisConfiguration(props);
            
            log.info("Initializing LettuceConnectionFactory with host: {}, port: {}", props.getHost(), props.getPort());

            RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
            redisConfig.setHostName(props.getHost());
            redisConfig.setPort(props.getPort());
            
            configureAuthentication(redisConfig, props);
            redisConfig.setDatabase(props.getDatabase());

            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder()
                    .commandTimeout(props.getCommandTimeout() != null ? props.getCommandTimeout() : Duration.ofSeconds(5))
                    .shutdownTimeout(Duration.ZERO);

            if (Boolean.TRUE.equals(props.getUseSsl())) {
                configureSsl(clientConfigBuilder, props);
            }

            if (props.getReadStrategy() != null && Boolean.TRUE.equals(props.getReadStrategy().getReadFromReplica())) {
                ReadFrom readFrom = getReadFromStrategy(props.getReadStrategy().getReplicaPreference());
                clientConfigBuilder.readFrom(readFrom);
                log.info("Read-from-replica enabled with strategy: {}", props.getReadStrategy().getReplicaPreference());
            }

            boolean autoReconnect = props.getAutoReconnect() != null ? props.getAutoReconnect() : true;
            LettuceClientConfiguration clientConfig = clientConfigBuilder
                    .clientOptions(ClientOptions.builder()
                            .autoReconnect(autoReconnect)
                            .pingBeforeActivateConnection(autoReconnect)
                            .build())
                    .build();
          

            LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
            factory.afterPropertiesSet();
            log.info("LettuceConnectionFactory initialized successfully");
            return factory;
        } catch (IllegalArgumentException e) {
            log.error("Redis configuration validation failed: {}", e.getMessage());
            throw new CacheConnectionException(
                    "Redis configuration validation failed: " + e.getMessage(),
                    props != null ? props.getHost() : "unknown",
                    props != null ? props.getPort() : null,
                    e);
        } catch (Exception e) {
            log.error("Failed to initialize LettuceConnectionFactory: {}", e.getMessage(), e);
            throw new CacheConnectionException(
                    "Failed to initialize LettuceConnectionFactory: " + e.getMessage(),
                    props != null ? props.getHost() : "unknown",
                    props != null ? props.getPort() : null,
                    e);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        try {
            if (factory == null) {
                throw new CacheConnectionException(
                        "RedisConnectionFactory is not properly initialized. " +
                        "Ensure Redis configuration is valid and connection can be established.",
                        null,
                        0,
                        new IllegalStateException("RedisConnectionFactory is null"));
            }
            
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);

            GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());

            template.setValueSerializer(serializer);
            template.setHashValueSerializer(serializer);

            template.afterPropertiesSet();

            log.info("RedisTemplate initialized with custom Jackson2JsonRedisSerializer");
            return template;
        } catch (CacheConnectionException e) {
            log.error("Failed to initialize RedisTemplate: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to initialize RedisTemplate: {}", e.getMessage(), e);
            throw new CacheConnectionException(
                    "Failed to initialize RedisTemplate: " + e.getMessage(),
                    null,
                    0,
                    e);
        }
    }

    /**
     * Creates RedisCacheService bean that wraps RedisTemplate.
     * Applies resilience decorators (metrics, circuit breaker, stampede protection).
     * 
     * Note: This is a prototype bean - a new instance is created for each namespace.
     * 
     * @param redisTemplate configured RedisTemplate
     * @param meterRegistry meter registry for metrics
     * @param redisProperties Redis configuration properties
     * @param redissonClient optional Redisson client for distributed locks
     * @return configured and decorated RedisCacheService
     */
    @Bean
    public com.immortals.cache.core.CacheService<String, Object> redisCacheService(
            RedisTemplate<String, Object> redisTemplate,
            io.micrometer.core.instrument.MeterRegistry meterRegistry,
            RedisProperties redisProperties,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RedissonClient redissonClient) {
        try {
            // Validate configuration
            validateRedisConfiguration(redisProperties);
            
            log.info("Creating RedisCacheService with namespace: default, ttl: {}", 
                    redisProperties.getTimeToLive());
            
            // Create base cache service (namespace-agnostic)
            RedisCacheService<String, Object> baseCache = new RedisCacheService<>(
                    redisTemplate, meterRegistry, redisProperties);
            
            // Apply resilience decorators
            DecoratorChainFactory decoratorFactory = new DecoratorChainFactory(
                    meterRegistry,
                    redissonClient,
                    redisProperties.getResilience().getStampedeProtection().getLockTimeout(),
                    Duration.ofSeconds(5),  // Default computation timeout
                    redisProperties.getResilience().getCircuitBreaker().getFailureRateThreshold(),
                    redisProperties.getResilience().getCircuitBreaker().getWaitDurationInOpenState()
            );
            
            boolean enableMetrics = true;
            boolean enableStampedeProtection = Boolean.TRUE.equals(
                    redisProperties.getResilience().getStampedeProtection().getEnabled());
            boolean enableCircuitBreaker = Boolean.TRUE.equals(
                    redisProperties.getResilience().getCircuitBreaker().getEnabled());
            
            com.immortals.cache.core.CacheService<String, Object> decorated = decoratorFactory.buildDecoratorChain(
                    baseCache,
                    "default",
                    enableMetrics,
                    enableStampedeProtection,
                    enableCircuitBreaker,
                    null  // No fallback cache for standalone Redis
            );
            
            log.info("RedisCacheService created with decorators: metrics={}, stampedeProtection={}, circuitBreaker={}",
                    enableMetrics, enableStampedeProtection, enableCircuitBreaker);
            
            return decorated;
        } catch (IllegalArgumentException e) {
            log.error("Redis configuration validation failed: {}", e.getMessage());
            throw new com.immortals.cache.core.exception.CacheConnectionException(
                    "Redis configuration validation failed: " + e.getMessage(), 
                    redisProperties != null ? redisProperties.getHost() : "unknown",
                    redisProperties != null ? redisProperties.getPort() : null,
                    e);
        } catch (Exception e) {
            log.error("Failed to create RedisCacheService: {}", e.getMessage(), e);
            throw new com.immortals.cache.core.exception.CacheConnectionException(
                    "Failed to create RedisCacheService: " + e.getMessage(), 
                    redisProperties != null ? redisProperties.getHost() : "unknown",
                    redisProperties != null ? redisProperties.getPort() : null,
                    e);
        }
    }

    /**
     * Validates Redis configuration properties for Standalone mode.
     * 
     * @param props Redis properties to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateRedisConfiguration(RedisProperties props) {
        if (props == null) {
            throw new IllegalArgumentException("RedisProperties cannot be null");
        }
        
        if (props.getHost() == null || props.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Redis host cannot be empty. Please set 'cache.redis.host' property");
        }
        
        if (props.getPort() <= 0 || props.getPort() > 65535) {
            throw new IllegalArgumentException("Redis port must be between 1 and 65535. Current value: " + props.getPort());
        }
        
        log.debug("Redis configuration validated: host={}, port={}, database={}", 
                props.getHost(), props.getPort(), props.getDatabase());
    }

    /**
     * Configures authentication based on ACL or password settings.
     */
    private void configureAuthentication(RedisStandaloneConfiguration redisConfig, RedisProperties props) {
        if (Boolean.TRUE.equals(props.getAcl().getEnabled()) && StringUtils.hasText(props.getAcl().getUsername())) {
            redisConfig.setUsername(props.getAcl().getUsername());
            if (props.getPassword() != null && !props.getPassword().isBlank()) {
                redisConfig.setPassword(props.getPassword());
            }
            log.info("Redis ACL authentication enabled for user: {}", props.getAcl().getUsername());
        } else if (props.getPassword() != null && !props.getPassword().isBlank()) {
            redisConfig.setPassword(props.getPassword());
            log.info("Redis password authentication enabled");
        }
    }

    /**
     * Converts the string-based replica preference configuration to Lettuce ReadFrom strategy.
     */
    private ReadFrom getReadFromStrategy(String preference) {
        return switch (preference.toUpperCase()) {
            case "REPLICA_PREFERRED" -> ReadFrom.REPLICA_PREFERRED;
            case "REPLICA" -> ReadFrom.REPLICA;
            case "MASTER" -> ReadFrom.MASTER;
            case "MASTER_PREFERRED" -> ReadFrom.MASTER_PREFERRED;
            case "ANY" -> ReadFrom.ANY;
            case "ANY_REPLICA" -> ReadFrom.ANY_REPLICA;
            default -> {
                log.warn("Unknown read strategy: {}, defaulting to REPLICA_PREFERRED", preference);
                yield ReadFrom.REPLICA_PREFERRED;
            }
        };
    }

    /**
     * Configures SSL/TLS for Lettuce client with support for custom trust stores and key stores.
     * Includes retry logic for SSL configuration failures.
     * 
     * @param clientConfigBuilder the Lettuce client configuration builder
     * @param props the cache properties containing SSL configuration
     */
    private void configureSsl(LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder, 
                             RedisProperties props) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < SSL_RETRY_ATTEMPTS) {
            try {
                SslOptions.Builder sslOptionsBuilder = SslOptions.builder();
                
                if (StringUtils.hasText(props.getSsl().getTrustStore())) {
                    TrustManagerFactory trustManagerFactory = createTrustManagerFactory(
                        props.getSsl().getTrustStore(),
                        props.getSsl().getTrustStorePassword()
                    );
                    sslOptionsBuilder.trustManager(trustManagerFactory);
                    log.info("SSL trust store configured: {}", props.getSsl().getTrustStore());
                }
                
                if (StringUtils.hasText(props.getSsl().getKeyStore())) {
                    KeyManagerFactory keyManagerFactory = createKeyManagerFactory(
                        props.getSsl().getKeyStore(),
                        props.getSsl().getKeyStorePassword()
                    );
                    sslOptionsBuilder.keyManager(keyManagerFactory);
                    log.info("SSL key store configured for mutual TLS: {}", props.getSsl().getKeyStore());
                }
                
                SslOptions sslOptions = sslOptionsBuilder.build();
                clientConfigBuilder.useSsl().and().clientOptions(
                    ClientOptions.builder()
                        .sslOptions(sslOptions)
                        .build()
                );
                
                log.info("SSL/TLS enabled for Redis connection");
                return;
            } catch (Exception e) {
                lastException = e;
                attempts++;
                log.warn("SSL/TLS configuration attempt {} failed: {}", attempts, e.getMessage());
                if (attempts < SSL_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(100 * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        log.error("Failed to configure SSL/TLS after {} attempts", SSL_RETRY_ATTEMPTS, lastException);
        throw new CacheConnectionException(
                "SSL/TLS configuration failed after " + SSL_RETRY_ATTEMPTS + " attempts: " + 
                (lastException != null ? lastException.getMessage() : "unknown error"),
                props != null ? props.getHost() : "unknown",
                props != null ? props.getPort() : null,
                lastException);
    }

    /**
     * Creates a TrustManagerFactory from the specified trust store.
     */
    private TrustManagerFactory createTrustManagerFactory(String trustStorePath, String trustStorePassword) 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
            trustStore.load(trustStoreStream, 
                trustStorePassword != null ? trustStorePassword.toCharArray() : null);
        }
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        trustManagerFactory.init(trustStore);
        return trustManagerFactory;
    }

    /**
     * Creates a KeyManagerFactory from the specified key store (for mutual TLS).
     */
    private KeyManagerFactory createKeyManagerFactory(String keyStorePath, String keyStorePassword) 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, 
                   UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream keyStoreStream = new FileInputStream(keyStorePath)) {
            keyStore.load(keyStoreStream, 
                keyStorePassword != null ? keyStorePassword.toCharArray() : null);
        }
        
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        );
        keyManagerFactory.init(keyStore, 
            keyStorePassword != null ? keyStorePassword.toCharArray() : null);
        return keyManagerFactory;
    }
}
