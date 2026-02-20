package com.immortals.platform.cache.config;

import com.immortals.platform.cache.providers.redis.RedisHashCacheService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Auto-configuration for Redis Hash Cache Service.
 * 
 * <p>This configuration is activated when:
 * <ul>
 *   <li>RedisTemplate is available in the context</li>
 *   <li>Redis classes are on the classpath</li>
 *   <li>No custom RedisHashCacheService bean is defined</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass({RedisTemplate.class})
@ConditionalOnBean(RedisTemplate.class)
public class RedisHashAutoConfiguration {

    /**
     * Creates a RedisHashCacheService bean for hash-based caching operations.
     * 
     * @param redisTemplate the Redis template for operations
     * @param meterRegistry the meter registry for metrics
     * @return configured RedisHashCacheService instance
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisHashCacheService<String, String, Object> redisHashCacheService(
            RedisTemplate<String, Object> redisTemplate,
            MeterRegistry meterRegistry) {
        return new RedisHashCacheService<>(redisTemplate, meterRegistry);
    }
}