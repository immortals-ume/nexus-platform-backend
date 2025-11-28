package com.immortals.cache.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of UnifiedCacheManager.
 * 
 * <p>Manages multiple cache instances with namespace isolation.
 * Each namespace has its own cache instance with independent configuration.
 * 
 * <p>This implementation:
 * <ul>
 *   <li>Creates cache instances on-demand (lazy initialization)</li>
 *   <li>Caches instances for reuse</li>
 *   <li>Supports namespace-specific configuration</li>
 *   <li>Applies decorator chains based on configuration</li>
 *   <li>Provides aggregated statistics across all namespaces</li>
 * </ul>
 * 
 * <p>Requirements: 1.5, 8.1, 8.2, 8.3, 8.4
 * 
 * @since 2.0.0
 */
public class DefaultUnifiedCacheManager implements UnifiedCacheManager {

    private final CacheService<?, ?> sharedCacheInstance;
    private final CacheConfiguration defaultConfiguration;
    private final DecoratorChainBuilder decoratorChainBuilder;
    private final Map<String, CacheService<?, ?>> cachedNamespacedInstances;

    /**
     * Creates a DefaultUnifiedCacheManager with a shared cache instance.
     * 
     * @param sharedCacheInstance the shared cache instance for all namespaces
     * @param defaultConfiguration default configuration for all caches
     * @param decoratorChainBuilder builder for applying decorators
     */
    public DefaultUnifiedCacheManager(
            CacheService<?, ?> sharedCacheInstance,
            CacheConfiguration defaultConfiguration,
            DecoratorChainBuilder decoratorChainBuilder) {
        this.sharedCacheInstance = sharedCacheInstance;
        this.defaultConfiguration = defaultConfiguration != null ? defaultConfiguration : new CacheConfiguration();
        this.decoratorChainBuilder = decoratorChainBuilder != null ? decoratorChainBuilder : new NoOpDecoratorChainBuilder();
        this.cachedNamespacedInstances = new ConcurrentHashMap<>();
    }

    /**
     * Get the cache instance for the given namespace.
     * 
     * <p>Returns a NamespacedCacheService wrapping the shared cache instance.
     * Wrapped instances are cached to ensure the same instance is returned
     * for repeated calls with the same namespace.
     * 
     * @param namespace the namespace identifier for the cache
     * @return the namespaced cache service instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> CacheService<K, V> getCache(String namespace) {
        validateNamespace(namespace);
        return (CacheService<K, V>) cachedNamespacedInstances.computeIfAbsent(namespace, ns -> {
            NamespacedCacheService<String, Object> wrapped =
                    new NamespacedCacheService<>((CacheService<String, Object>) sharedCacheInstance, ns);
            return decoratorChainBuilder.buildDecoratorChain(wrapped, ns, defaultConfiguration);
        });
    }

    /**
     * Get the cache instance for the given namespace with specific configuration.
     * 
     * <p>Returns a NamespacedCacheService wrapping the shared cache instance.
     * Wrapped instances are cached to ensure the same instance is returned
     * for repeated calls with the same namespace.
     * 
     * @param namespace the namespace identifier for the cache
     * @param config the configuration for the cache
     * @return the namespaced cache service instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public <K, V> CacheService<K, V> getCache(String namespace, CacheConfiguration config) {
        validateNamespace(namespace);
        validateConfiguration(config);
        return (CacheService<K, V>) cachedNamespacedInstances.computeIfAbsent(namespace, ns -> {
            NamespacedCacheService<String, Object> wrapped =
                    new NamespacedCacheService<>((CacheService<String, Object>) sharedCacheInstance, ns);
            return decoratorChainBuilder.buildDecoratorChain(wrapped, ns, config);
        });
    }

    /**
     * Remove a cache namespace from the cache.
     * 
     * @param namespace the namespace identifier to remove
     */
    @Override
    public void removeCache(String namespace) {
        validateNamespace(namespace);
        cachedNamespacedInstances.remove(namespace);
    }

    /**
     * Get all registered cache namespace names.
     * 
     * @return collection of namespace names that have been cached
     */
    @Override
    public Collection<String> getCacheNames() {
        return cachedNamespacedInstances.keySet();
    }

    /**
     * Get aggregated statistics across all caches.
     * 
     * @return statistics for the shared cache instance
     */
    @Override
    public Map<String, CacheStatistics> getAllStatistics() {
        Map<String, CacheStatistics> stats = new ConcurrentHashMap<>();
        try {
            CacheStatistics cacheStats = sharedCacheInstance.getStatistics();
            stats.put("shared", cacheStats);
        } catch (Exception e) {
        }
        return stats;
    }



    /**
     * Validates namespace is not null or empty.
     */
    private void validateNamespace(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }
    }

    /**
     * Validates configuration is not null.
     */
    private void validateConfiguration(CacheConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
    }

    /**
     * Interface for building decorator chains.
     */
    public interface DecoratorChainBuilder {
        <K, V> CacheService<K, V> buildDecoratorChain(
                CacheService<K, V> baseCache,
                String namespace,
                CacheConfiguration config
        );
    }

    /**
     * No-op decorator chain builder that returns the base cache unchanged.
     */
    private static class NoOpDecoratorChainBuilder implements DecoratorChainBuilder {
        @Override
        public <K, V> CacheService<K, V> buildDecoratorChain(
                CacheService<K, V> baseCache,
                String namespace,
                CacheConfiguration config) {
            return baseCache;
        }
    }
}
