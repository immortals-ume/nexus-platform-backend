package com.immortals.cache.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultUnifiedCacheManager namespace caching behavior.
 */
@DisplayName("DefaultUnifiedCacheManager")
class DefaultUnifiedCacheManagerTest {

    private DefaultUnifiedCacheManager cacheManager;
    private MockCacheService mockCacheService;

    @BeforeEach
    void setUp() {
        mockCacheService = new MockCacheService();
        cacheManager = new DefaultUnifiedCacheManager(
                mockCacheService,
                new CacheConfiguration(),
                new DefaultUnifiedCacheManager.DecoratorChainBuilder() {
                    @Override
                    public <K, V> CacheService<K, V> buildDecoratorChain(
                            CacheService<K, V> baseCache,
                            String namespace,
                            CacheConfiguration config) {
                        return baseCache;
                    }
                }
        );
    }

    @Test
    @DisplayName("should return same wrapped instance for repeated calls with same namespace")
    void testSameInstanceReturnedForSameNamespace() {
        // Act
        CacheService<String, Object> cache1 = cacheManager.getCache("users");
        CacheService<String, Object> cache2 = cacheManager.getCache("users");

        // Assert
        assertSame(cache1, cache2, "Same namespace should return the same cached instance");
    }

    @Test
    @DisplayName("should return different wrapped instances for different namespaces")
    void testDifferentInstancesForDifferentNamespaces() {
        // Act
        CacheService<String, Object> usersCache = cacheManager.getCache("users");
        CacheService<String, Object> productsCache = cacheManager.getCache("products");

        // Assert
        assertNotSame(usersCache, productsCache, "Different namespaces should return different instances");
    }

    @Test
    @DisplayName("should wrap cache with NamespacedCacheService")
    void testCacheIsWrappedWithNamespacedCacheService() {
        // Act
        CacheService<String, Object> cache = cacheManager.getCache("users");

        // Assert
        assertInstanceOf(NamespacedCacheService.class, cache, 
                "Cache should be wrapped with NamespacedCacheService");
    }

    @Test
    @DisplayName("should prefix keys with namespace when storing values")
    void testKeyPrefixingWhenStoringValues() {
        // Act
        CacheService<String, Object> cache = cacheManager.getCache("users");
        cache.put("user1", "John");

        // Assert
        assertTrue(mockCacheService.containsKey("users:user1"), 
                "Key should be prefixed with namespace");
    }

    @Test
    @DisplayName("should prefix keys with namespace when retrieving values")
    void testKeyPrefixingWhenRetrievingValues() {
        // Arrange
        mockCacheService.put("users:user1", "John");
        CacheService<String, Object> cache = cacheManager.getCache("users");

        // Act
        Optional<Object> result = cache.get("user1");

        // Assert
        assertTrue(result.isPresent(), "Should retrieve value with prefixed key");
        assertEquals("John", result.get(), "Should return correct value");
    }

    @Test
    @DisplayName("should isolate namespaces - same key in different namespaces")
    void testNamespaceIsolation() {
        // Arrange
        CacheService<String, Object> usersCache = cacheManager.getCache("users");
        CacheService<String, Object> productsCache = cacheManager.getCache("products");

        // Act
        usersCache.put("id1", "User1");
        productsCache.put("id1", "Product1");

        // Assert
        assertEquals("User1", usersCache.get("id1").get(), "Users namespace should have User1");
        assertEquals("Product1", productsCache.get("id1").get(), "Products namespace should have Product1");
    }

    @Test
    @DisplayName("should track cached namespaces in getCacheNames")
    void testGetCacheNamesTracksNamespaces() {
        // Act
        cacheManager.getCache("users");
        cacheManager.getCache("products");

        // Assert
        assertTrue(cacheManager.getCacheNames().contains("users"), "Should track users namespace");
        assertTrue(cacheManager.getCacheNames().contains("products"), "Should track products namespace");
        assertEquals(2, cacheManager.getCacheNames().size(), "Should have 2 namespaces");
    }

    @Test
    @DisplayName("should remove namespace from cache")
    void testRemoveCache() {
        // Arrange
        cacheManager.getCache("users");
        assertTrue(cacheManager.getCacheNames().contains("users"));

        // Act
        cacheManager.removeCache("users");

        // Assert
        assertFalse(cacheManager.getCacheNames().contains("users"), "Namespace should be removed");
    }

    @Test
    @DisplayName("should throw exception for null namespace")
    void testNullNamespaceThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> cacheManager.getCache(null),
                "Should throw exception for null namespace");
    }

    @Test
    @DisplayName("should throw exception for empty namespace")
    void testEmptyNamespaceThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> cacheManager.getCache(""),
                "Should throw exception for empty namespace");
    }

    /**
     * Mock cache service for testing.
     */
    private static class MockCacheService implements CacheService<String, Object> {
        private final java.util.Map<String, Object> store = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public void put(String key, Object value) {
            store.put(key, value);
        }

        @Override
        public void put(String key, Object value, java.time.Duration ttl) {
            store.put(key, value);
        }

        @Override
        public Optional<Object> get(String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void remove(String key) {
            store.remove(key);
        }

        @Override
        public void clear() {
            store.clear();
        }

        @Override
        public boolean containsKey(String key) {
            return store.containsKey(key);
        }

        @Override
        public void putAll(java.util.Map<String, Object> entries) {
            store.putAll(entries);
        }

        @Override
        public java.util.Map<String, Object> getAll(java.util.Collection<String> keys) {
            return keys.stream()
                    .filter(store::containsKey)
                    .collect(java.util.stream.Collectors.toMap(k -> k, store::get));
        }

        @Override
        public boolean putIfAbsent(String key, Object value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public boolean putIfAbsent(String key, Object value, java.time.Duration ttl) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Long increment(String key, long delta) {
            Long current = (Long) store.getOrDefault(key, 0L);
            Long newValue = current + delta;
            store.put(key, newValue);
            return newValue;
        }

        @Override
        public Long decrement(String key, long delta) {
            Long current = (Long) store.getOrDefault(key, 0L);
            Long newValue = current - delta;
            store.put(key, newValue);
            return newValue;
        }

        @Override
        public CacheStatistics getStatistics() {
            return CacheStatistics.builder()
                    .namespace("mock")
                    .timestamp(java.time.Instant.now())
                    .window(CacheStatistics.TimeWindow.ALL_TIME)
                    .hitCount(0)
                    .missCount(0)
                    .build();
        }
    }
}
