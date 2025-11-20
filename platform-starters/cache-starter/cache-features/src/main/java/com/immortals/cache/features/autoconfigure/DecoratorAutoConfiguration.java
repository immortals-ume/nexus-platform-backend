package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.core.DefaultUnifiedCacheManager;
import com.immortals.cache.providers.caffeine.CaffeineProperties;
import com.immortals.cache.providers.multilevel.MultiLevelCacheProperties;
import com.immortals.cache.providers.redis.RedisProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Auto-configuration for cache decorator features.
 * 
 * <p>This configuration ensures that all necessary beans are available for
 * the decorator chain builder to apply features like:
 * <ul>
 *   <li>Compression - reduces memory and network usage</li>
 *   <li>Encryption - secures data at rest</li>
 *   <li>Circuit Breaker - provides fault tolerance</li>
 *   <li>Stampede Protection - prevents cache avalanche</li>
 * </ul>
 * 
 * <p>Decorators are applied automatically by the DefaultDecoratorChainBuilder
 * based on configuration properties. This class provides the infrastructure
 * beans needed by the decorators.
 * 
 * <p>Requirements: 6.1, 6.2, 7.1, 7.2
 * 
 * @since 2.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({
        CacheProperties.class,
        RedisProperties.class,
        CaffeineProperties.class,
        MultiLevelCacheProperties.class
})
public class DecoratorAutoConfiguration {
    
    /**
     * Creates RedissonClient for distributed locking (stampede protection).
     * 
     * <p>RedissonClient is used by StampedeProtectionDecorator to acquire
     * distributed locks and prevent multiple threads/instances from loading
     * the same cache entry simultaneously.
     * 
     * <p>This bean is only created when:
     * <ul>
     *   <li>Redisson is on the classpath</li>
     *   <li>Stampede protection is enabled</li>
     *   <li>No custom RedissonClient bean exists</li>
     * </ul>
     * 
     * @param cacheProperties cache properties
     * @return configured RedissonClient
     */
    @Bean
    @ConditionalOnClass(name = "org.redisson.Redisson")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        name = "immortals.cache.resilience.stampede-protection.enabled",
        havingValue = "true"
    )
    public RedissonClient redissonClient(CacheProperties cacheProperties) {
        log.info("Configuring RedissonClient for stampede protection");
        RedisProperties redisConfig = cacheProperties.getRedisProperties();
        
        Config config = new Config();
        
        // Configure based on deployment mode
        if (!redisConfig.getCluster().getNodes().isEmpty()) {
            // Cluster mode
            config.useClusterServers()
                    .addNodeAddress(redisConfig.getCluster().getNodes().stream()
                            .map(node -> "redis://" + node)
                            .toArray(String[]::new));
            
            if (redisConfig.getPassword() != null) {
                config.useClusterServers().setPassword(redisConfig.getPassword());
            }
            
            log.info("RedissonClient configured in cluster mode");
            
        } else if (redisConfig.getSentinel().getMaster() != null) {
            // Sentinel mode
            config.useSentinelServers()
                    .setMasterName(redisConfig.getSentinel().getMaster())
                    .addSentinelAddress(redisConfig.getSentinel().getNodes().stream()
                            .map(node -> "redis://" + node)
                            .toArray(String[]::new));
            
            if (redisConfig.getPassword() != null) {
                config.useSentinelServers().setPassword(redisConfig.getPassword());
            }
            
            config.useSentinelServers().setDatabase(redisConfig.getDatabase());
            
            log.info("RedissonClient configured in sentinel mode");
            
        } else {
            // Standalone mode
            String address = (redisConfig.isSslEnabled() ? "rediss://" : "redis://")
                    + redisConfig.getHost() + ":" + redisConfig.getPort();
            
            config.useSingleServer()
                    .setAddress(address)
                    .setDatabase(redisConfig.getDatabase());
            
            if (redisConfig.getPassword() != null) {
                config.useSingleServer().setPassword(redisConfig.getPassword());
            }
            
            if (redisConfig.getAcl() != null && redisConfig.getAcl().getEnabled() != null && redisConfig.getAcl().getEnabled() && redisConfig.getAcl().getUsername() != null) {
                config.useSingleServer().setUsername(redisConfig.getAcl().getUsername());
            }
            
            log.info("RedissonClient configured in standalone mode: {}", address);
        }
        
        return Redisson.create(config);
    }
    
    /**
     * Validates encryption configuration at startup.
     * 
     * <p>Ensures that if encryption is enabled, a valid encryption key is provided.
     * Fails fast at startup if configuration is invalid.
     * 
     * @param cacheProperties cache properties
     */
    @Bean
    @ConditionalOnProperty(
        name = "immortals.cache.features.encryption.enabled",
        havingValue = "true"
    )
    public EncryptionConfigurationValidator encryptionConfigurationValidator(
            CacheProperties cacheProperties) {
        
        log.info("Validating encryption configuration");
        
        String encryptionKey = cacheProperties.getFeatures().getEncryption().getKey();
        
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            String errorMsg = "Encryption is enabled but no encryption key is provided. " +
                    "Please set 'immortals.cache.features.encryption.key' property.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        // Validate key is Base64 encoded
        try {
            java.util.Base64.getDecoder().decode(encryptionKey);
        } catch (IllegalArgumentException e) {
            String errorMsg = "Encryption key must be Base64 encoded. " +
                    "Please provide a valid Base64-encoded key in 'immortals.cache.features.encryption.key' property.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
        
        log.info("Encryption configuration validated successfully");
        
        return new EncryptionConfigurationValidator();
    }
    
    /**
     * Marker class for encryption configuration validation.
     */
    public static class EncryptionConfigurationValidator {
        // Marker class - validation happens in bean creation
    }
    
    /**
     * Logs decorator configuration summary at startup.
     * 
     * @param cacheProperties cache properties
     * @param meterRegistry meter registry
     * @return decorator configuration summary bean
     */
    @Bean
    public DecoratorConfigurationSummary decoratorConfigurationSummary(
            CacheProperties cacheProperties,
            MeterRegistry meterRegistry) {
        
        log.info("=== Cache Decorator Configuration Summary ===");
        log.info("Compression: {} (threshold: {} bytes)", 
                cacheProperties.getFeatures().getCompression().isEnabled(),
                cacheProperties.getFeatures().getCompression().getThreshold());
        log.info("Encryption: {} (algorithm: {})", 
                cacheProperties.getFeatures().getEncryption().isEnabled(),
                cacheProperties.getFeatures().getEncryption().getAlgorithm());
        log.info("Circuit Breaker: {} (failure threshold: {}%)", 
                cacheProperties.getResilience().getCircuitBreaker().isEnabled(),
                cacheProperties.getResilience().getCircuitBreaker().getFailureRateThreshold());
        log.info("Stampede Protection: {} (lock timeout: {})", 
                cacheProperties.getResilience().getStampedeProtection().isEnabled(),
                cacheProperties.getResilience().getStampedeProtection().getLockTimeout());
        log.info("Metrics: {}", 
                cacheProperties.getObservability().getMetrics().isEnabled());
        log.info("Tracing: {}", 
                cacheProperties.getObservability().getTracing().isEnabled());
        log.info("============================================");
        
        return new DecoratorConfigurationSummary();
    }
    
    /**
     * Marker class for decorator configuration summary.
     */
    public static class DecoratorConfigurationSummary {

    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultUnifiedCacheManager.DecoratorChainBuilder decoratorChainBuilder(
            CacheProperties cacheProperties,
            MeterRegistry meterRegistry,
            ObjectProvider<RedissonClient> redissonClientProvider) {

        log.info("Creating DefaultDecoratorChainBuilder");

        RedissonClient redissonClient = redissonClientProvider.getIfAvailable(); // may be null

        String encryptionKey = cacheProperties.getFeatures().getEncryption().isEnabled()
                ? cacheProperties.getFeatures().getEncryption().getKey()
                : null;

        int compressionThreshold = cacheProperties.getFeatures().getCompression().isEnabled()
                ? cacheProperties.getFeatures().getCompression().getThreshold()
                : 0;

        Duration stampedeTimeout = cacheProperties.getResilience().getStampedeProtection().isEnabled()
                ? cacheProperties.getResilience().getStampedeProtection().getLockTimeout()
                : null;

        boolean circuitBreakerEnabled = cacheProperties.getResilience().getCircuitBreaker().isEnabled();

        log.debug("Decorator configuration: encryption={}, compression={}, stampede={}, circuitBreaker={}",
                encryptionKey != null,
                compressionThreshold > 0,
                stampedeTimeout != null,
                circuitBreakerEnabled);

        return new DefaultDecoratorChainBuilder(
                meterRegistry,
                redissonClient,
                encryptionKey,
                compressionThreshold,
                stampedeTimeout,
                circuitBreakerEnabled
        );
    }

}
