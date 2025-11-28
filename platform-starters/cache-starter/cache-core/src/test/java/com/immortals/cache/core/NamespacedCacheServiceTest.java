package com.immortals.cache.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NamespacedCacheService key prefixing behavior.
 * Verifies that keys are properly prefixed with namespace identifiers
 * and that different namespaces don't collide.
 */
@DisplayName("NamespacedCacheService")
class NamespacedCacheServiceTest {

    private MockCacheService mockDelegate;
    private NamespacedCacheService<String, String> namespacedCache;
    private NamespacedCacheService<String, String> anotherNamespacedCache;

    @BeforeEach
    void setUp() {
        mockDelegate = new MockCacheService();
        namespacedCache = new NamespacedCacheService<String, String>(mockDelegate, "namespace1");
        anotherNamespacedCache = new NamespacedCacheService<String, String>(mockDelegate, "namespace2");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on put")
    void testKeyPrefixingOnPut() {
        namespacedCache.put("key1", "value1");

        assertTrue(mockDelegate.containsKey("namespace1:key1"),
                "Key should be prefixed with namespace identifier");
        assertEquals("value1", mockDelegate.get("namespace1:key1").get(),
                "Value should be stored with prefixed key");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on get")
    void testKeyPrefixingOnGet() {
        mockDelegate.put("namespace1:key1", "value1");

        Optional<String> result = namespacedCache.get("key1");

        assertTrue(result.isPresent(), "Should retrieve value with prefixed key");
        assertEquals("value1", result.get(), "Should return correct value");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on containsKey")
    void testKeyPrefixingOnContainsKey() {
        mockDelegate.put("namespace1:key1", "value1");

        boolean exists = namespacedCache.containsKey("key1");

        assertTrue(exists, "Should find key with namespace prefix");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on remove")
    void testKeyPrefixingOnRemove() {
        mockDelegate.put("namespace1:key1", "value1");

        namespacedCache.remove("key1");

        assertFalse(mockDelegate.containsKey("namespace1:key1"),
                "Key should be removed with namespace prefix");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on putIfAbsent")
    void testKeyPrefixingOnPutIfAbsent() {
        boolean result = namespacedCache.putIfAbsent("key1", "value1");

        assertTrue(result, "Should return true for new key");
        assertTrue(mockDelegate.containsKey("namespace1:key1"),
                "Key should be prefixed with namespace identifier");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on increment")
    void testKeyPrefixingOnIncrement() {
        namespacedCache.increment("counter", 5);

        assertTrue(mockDelegate.containsKey("namespace1:counter"),
                "Key should be prefixed with namespace identifier");
        assertEquals("5", mockDelegate.get("namespace1:counter").get(),
                "Value should be incremented with prefixed key");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on decrement")
    void testKeyPrefixingOnDecrement() {
        mockDelegate.put("namespace1:counter", "10");

        Long result = namespacedCache.decrement("counter", 3);

        assertEquals(7L, result, "Should decrement value with prefixed key");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on putAll")
    void testKeyPrefixingOnPutAll() {
        Map<String, String> entries = new HashMap<>();
        entries.put("key1", "value1");
        entries.put("key2", "value2");

        namespacedCache.putAll(entries);

        assertTrue(mockDelegate.containsKey("namespace1:key1"),
                "First key should be prefixed with namespace");
        assertTrue(mockDelegate.containsKey("namespace1:key2"),
                "Second key should be prefixed with namespace");
    }

    @Test
    @DisplayName("should prefix keys with namespace identifier on getAll")
    void testKeyPrefixingOnGetAll() {
        mockDelegate.put("namespace1:key1", "value1");
        mockDelegate.put("namespace1:key2", "value2");

        Map<String, String> results = namespacedCache.getAll(Arrays.asList("key1", "key2"));

        assertEquals(2, results.size(), "Should retrieve both values");
        assertEquals("value1", results.get("key1"), "Should return value for key1");
        assertEquals("value2", results.get("key2"), "Should return value for key2");
    }

    @Test
    @DisplayName("should prevent key collision between different namespaces")
    void testNamespaceCollisionPrevention() {
        namespacedCache.put("key1", "value_namespace1");
        anotherNamespacedCache.put("key1", "value_namespace2");

        assertEquals("value_namespace1", namespacedCache.get("key1").get(),
                "Namespace1 should have its own value");
        assertEquals("value_namespace2", anotherNamespacedCache.get("key1").get(),
                "Namespace2 should have its own value");
        assertTrue(mockDelegate.containsKey("namespace1:key1"),
                "Namespace1 key should exist in delegate");
        assertTrue(mockDelegate.containsKey("namespace2:key1"),
                "Namespace2 key should exist in delegate");
    }

    @Test
    @DisplayName("should isolate namespaces - same key different values")
    void testNamespaceIsolationWithSameKey() {
        mockDelegate.put("namespace1:id", "user123");
        mockDelegate.put("namespace2:id", "product456");

        String namespace1Value = namespacedCache.get("id").get();
        String namespace2Value = anotherNamespacedCache.get("id").get();

        assertEquals("user123", namespace1Value, "Namespace1 should retrieve its value");
        assertEquals("product456", namespace2Value, "Namespace2 should retrieve its value");
    }

    @Test
    @DisplayName("should be transparent to callers - callers use original keys")
    void testTransparencyToCallers() {
        namespacedCache.put("user_key", "user_data");
        Optional<String> result = namespacedCache.get("user_key");

        assertTrue(result.isPresent(), "Should retrieve value using original key");
        assertEquals("user_data", result.get(), "Should return correct value");
        assertTrue(mockDelegate.containsKey("namespace1:user_key"),
                "Delegate should have prefixed key");
    }

    @Test
    @DisplayName("should handle put with TTL and prefix keys")
    void testKeyPrefixingWithTTL() {
        namespacedCache.put("key1", "value1", Duration.ofSeconds(60));

        assertTrue(mockDelegate.containsKey("namespace1:key1"),
                "Key should be prefixed with namespace identifier");
    }

    @Test
    @DisplayName("should handle putIfAbsent with TTL and prefix keys")
    void testKeyPrefixingPutIfAbsentWithTTL() {
        boolean result = namespacedCache.putIfAbsent("key1", "value1", Duration.ofSeconds(60));

        assertTrue(result, "Should return true for new key");
        assertTrue(mockDelegate.containsKey("namespace1:key1"),
                "Key should be prefixed with namespace identifier");
    }

    @Test
    @DisplayName("should throw exception for null key")
    void testNullKeyThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> namespacedCache.put(null, "value"),
                "Should throw exception for null key");
    }

    @Test
    @DisplayName("should throw exception for null delegate")
    void testNullDelegateThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new NamespacedCacheService<>(null, "namespace"),
                "Should throw exception for null delegate");
    }

    @Test
    @DisplayName("should throw exception for null namespace")
    void testNullNamespaceThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new NamespacedCacheService<>(mockDelegate, null),
                "Should throw exception for null namespace");
    }

    @Test
    @DisplayName("should throw exception for empty namespace")
    void testEmptyNamespaceThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new NamespacedCacheService<>(mockDelegate, ""),
                "Should throw exception for empty namespace");
    }

    /**
     * Mock cache service for testing.
     */
    private static class MockCacheService implements CacheService<String, String> {
        private final Map<String, String> store = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public void put(String key, String value) {
            store.put(key, value);
        }

        @Override
        public void put(String key, String value, Duration ttl) {
            store.put(key, value);
        }

        @Override
        public Optional<String> get(String key) {
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
        public void putAll(Map<String, String> entries) {
            store.putAll(entries);
        }

        @Override
        public Map<String, String> getAll(java.util.Collection<String> keys) {
            return keys.stream()
                    .filter(store::containsKey)
                    .collect(java.util.stream.Collectors.toMap(k -> k, store::get));
        }

        @Override
        public boolean putIfAbsent(String key, String value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public boolean putIfAbsent(String key, String value, Duration ttl) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public Long increment(String key, long delta) {
            Long current = Long.parseLong(store.getOrDefault(key, "0"));
            Long newValue = current + delta;
            store.put(key, newValue.toString());
            return newValue;
        }

        @Override
        public Long decrement(String key, long delta) {
            Long current = Long.parseLong(store.getOrDefault(key, "0"));
            Long newValue = current - delta;
            store.put(key, newValue.toString());
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
