package com.immortals.cache.features.annotations;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.UnifiedCacheManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for cache annotation support.
 * Enables declarative caching using @Cacheable, @CachePut, and @CacheEvict annotations.
 *
 * @since 2.0.0
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@ConditionalOnClass(CacheService.class)
@ConditionalOnProperty(
        prefix = "immortals.cache.annotations",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CacheAnnotationAutoConfiguration {

    /**
     * Creates the cache aspect bean that processes cache annotations.
     *
     * @param cacheManager the unified cache manager for namespace support
     * @return the cache aspect
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheAspect cacheAspect(UnifiedCacheManager cacheManager) {
        return new CacheAspect(cacheManager);
    }
}
