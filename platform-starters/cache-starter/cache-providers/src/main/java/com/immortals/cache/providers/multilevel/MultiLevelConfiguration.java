
package com.immortals.cache.providers.multilevel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immortals.cache.core.CacheService;
import com.immortals.cache.providers.caffeine.L1CacheService;
import com.immortals.cache.providers.caffeine.CaffeineProperties;
import com.immortals.cache.providers.redis.RedisCacheService;
import com.immortals.cache.providers.redis.RedisProperties;
import com.immortals.cache.providers.resilience.DecoratorChainFactory;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Configuration for multi-level cache.
 * 
 * <p>This configuration automatically wires together:
 * <ul>
 *   <li>L1 Cache: Caffeine (local in-memory)</li>
 *   <li>L2 Cache: Redis (distributed)</li>
 *   <li>Eviction Publisher: Redis Pub/Sub</li>
 *   <li>Eviction Subscriber: Listens for distributed eviction events</li>
 * </ul>
 * 
 * <p>This is imported by MultiLevelAutoConfiguration in cache-features.
 * 
 * @since 2.0.0
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "cache.multilevel", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({MultiLevelCacheProperties.class, CaffeineProperties.class, RedisProperties.class})
public class MultiLevelConfiguration {

    private static final String EVICTION_CHANNEL_PREFIX = "cache:eviction:";

    /**
     * Create L1 cache using Caffeine.
     * This is the fast local in-memory cache.
     */
    @Bean
    @Qualifier("l1Cache")
    public <K, V> CacheService<K, V> l1CacheService(
            CaffeineProperties caffeineProperties,
            MeterRegistry meterRegistry) {
        
        log.info("Creating L1 cache (Caffeine) with maxSize: {}, ttl: {}", 
                caffeineProperties.getMaximumSize(), 
                caffeineProperties.getTtl());
        
        return new L1CacheService<>(caffeineProperties, meterRegistry);
    }

    /**
     * Create L2 cache using Redis.
     * This is the distributed cache shared across instances.
     */
    @Bean
    @Qualifier("l2Cache")
    public <K, V> CacheService<K, V> l2CacheService(
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry,
            RedisProperties redisProperties) {
        
        log.info("Creating L2 cache (Redis) with ttl: {}, pipelining: {}", 
                redisProperties.getTimeToLive(),
                redisProperties.getPipelining().getEnabled());
        
        return new RedisCacheService<>(redisTemplate, meterRegistry, redisProperties);
    }

    /**
     * Create the eviction publisher for distributed cache invalidation.
     */
    @Bean
    public EvictionPublisher evictionPublisher(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        
        RedisEvictionPublisher publisher = new RedisEvictionPublisher(redisTemplate, objectMapper);
        log.info("Created eviction publisher with instance ID: {}", publisher.getInstanceId());
        return publisher;
    }

    /**
     * Create the multi-level cache service combining L1 and L2.
     * Applies resilience decorators (metrics, circuit breaker, stampede protection).
     * This is the main cache service that applications should use.
     */
    @Bean
    public <K, V> CacheService<K, V> multiLevelCacheService(
            @Qualifier("l1Cache") CacheService<K, V> l1Cache,
            @Qualifier("l2Cache") CacheService<K, V> l2Cache,
            EvictionPublisher evictionPublisher,
            MultiLevelCacheProperties properties,
            MeterRegistry meterRegistry,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RedissonClient redissonClient) {
        
        String namespace = "default";
        
        try {
            // Validate that both L1 and L2 caches are properly initialized
            validateMultiLevelConfiguration(l1Cache, l2Cache, evictionPublisher, properties);
            
            log.info("Creating multi-level cache service for namespace: {} with fallback: {}, eviction: {}", 
                    namespace,
                    properties.isFallbackEnabled(),
                    properties.isEvictionEnabled());
            
            // Create base multi-level cache service
            MultiLevelCacheService<K, V> baseCache = new MultiLevelCacheService<>(
                    l1Cache, l2Cache, evictionPublisher, namespace);
            
            // Apply resilience decorators
            DecoratorChainFactory decoratorFactory = new DecoratorChainFactory(
                    meterRegistry,
                    redissonClient,
                    Duration.ofSeconds(5),  // Default stampede lock timeout
                    Duration.ofSeconds(5),  // Default computation timeout
                    50,                      // Default circuit breaker failure threshold
                    Duration.ofSeconds(60)   // Default circuit breaker wait duration
            );
            
            boolean enableMetrics = true;
            boolean enableStampedeProtection = redissonClient != null;
            boolean enableCircuitBreaker = true;  // Enable circuit breaker for multi-level cache
            
            CacheService<K, V> decorated = decoratorFactory.buildDecoratorChain(
                    baseCache,
                    namespace,
                    enableMetrics,
                    enableStampedeProtection,
                    enableCircuitBreaker,
                    l1Cache  // Use L1 cache as fallback when L2 fails
            );
            
            log.info("Multi-level cache service created with decorators: metrics={}, stampedeProtection={}, circuitBreaker={}",
                    enableMetrics, enableStampedeProtection, enableCircuitBreaker);
            
            return decorated;
        } catch (com.immortals.cache.core.exception.CacheConfigurationException e) {
            log.error("Multi-level cache configuration validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to create multi-level cache service: {}", e.getMessage(), e);
            throw new com.immortals.cache.core.exception.CacheConfigurationException(e.getMessage());
        }
    }

    /**
     * Validates multi-level cache configuration.
     * Ensures both L1 and L2 caches are properly initialized.
     *
     * @param l1Cache the L1 cache service
     * @param l2Cache the L2 cache service
     * @param evictionPublisher the eviction publisher
     * @param properties the multi-level cache properties
     * @throws com.immortals.cache.core.exception.CacheConfigurationException if validation fails
     */
    private void validateMultiLevelConfiguration(
            CacheService<?,?> l1Cache,
            CacheService<?,?> l2Cache,
            EvictionPublisher evictionPublisher,
            MultiLevelCacheProperties properties) {
        
        if (l1Cache == null) {
            throw new com.immortals.cache.core.exception.CacheConfigurationException(
                    "L1 cache (Caffeine) is not properly initialized. " +
                    "Ensure Caffeine configuration is enabled and valid.",
                    "immortals.cache.caffeine"
            );
        }

        if (l2Cache == null) {
            throw new com.immortals.cache.core.exception.CacheConfigurationException(
                    "L2 cache (Redis) is not properly initialized. " +
                    "Ensure Redis configuration is enabled and valid.",
                    "immortals.cache.redis"
            );
        }

        if (evictionPublisher == null) {
            throw new com.immortals.cache.core.exception.CacheConfigurationException(
                    "Eviction publisher is not properly initialized. " +
                    "Ensure Redis is configured for multi-level cache.",
                    "immortals.cache.multi-level.eviction-publisher"
            );
        }

        if (properties == null) {
            throw new com.immortals.cache.core.exception.CacheConfigurationException(
                    "MultiLevelCacheProperties cannot be null",
                    "immortals.cache.multi-level"
            );
        }

        log.debug("Multi-level cache configuration validation passed: " +
                "l1Cache={}, l2Cache={}, evictionPublisher={}, fallbackEnabled={}, evictionEnabled={}",
                l1Cache.getClass().getSimpleName(),
                l2Cache.getClass().getSimpleName(),
                evictionPublisher.getClass().getSimpleName(),
                properties.isFallbackEnabled(),
                properties.isEvictionEnabled());
    }

    /**
     * Create the Redis message listener container for eviction events.
     */
    @Bean
    @ConditionalOnProperty(prefix = "cache.multilevel", name = "eviction-enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("l1Cache") CacheService<Object, Object> l1Cache,
            ObjectMapper objectMapper,
            EvictionPublisher evictionPublisher) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // Register eviction subscriber for the default namespace
        String namespace = "default";
        EvictionSubscriber subscriber = new EvictionSubscriber(
                l1Cache, 
                objectMapper, 
                evictionPublisher.getInstanceId(), 
                namespace);
        
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber);
        String channel = EVICTION_CHANNEL_PREFIX + namespace;
        container.addMessageListener(adapter, new ChannelTopic(channel));
        
        log.info("Registered eviction subscriber for namespace: {} on channel: {}", namespace, channel);
        
        return container;
    }
}
