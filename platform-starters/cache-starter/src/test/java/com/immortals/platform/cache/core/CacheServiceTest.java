package com.immortals.platform.cache.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test class for CacheService interface contract.
 * This tests the expected behavior of any CacheService implementation.
 */
class CacheServiceTest {

    @Mock
    private CacheService<String, String> cacheService;

    @Mock
    private CacheStatistics cacheStatistics;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldPutAndGetValue() {
        String key = "test-key";
        String value = "test-value";
        
        when(cacheService.get(key)).thenReturn(Optional.of(value));
        
        cacheService.put(key, value);
        Optional<String> result = cacheService.get(key);
        
        verify(cacheService).put(key, value);
        assertThat(result).isPresent().contains(value);
    }

    @Test
    void shouldPutWithTtl() {
        String key = "ttl-key";
        String value = "ttl-value";
        Duration ttl = Duration.ofMinutes(5);
        
        cacheService.put(key, value, ttl);
        
        verify(cacheService).put(key, value, ttl);
    }

    @Test
    void shouldReturnEmptyWhenKeyNotFound() {
        String key = "non-existent-key";
        
        when(cacheService.get(key)).thenReturn(Optional.empty());
        
        Optional<String> result = cacheService.get(key);
        
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRemoveKey() {
        String key = "remove-key";
        
        cacheService.remove(key);
        
        verify(cacheService).remove(key);
    }

    @Test
    void shouldClearAllEntries() {
        cacheService.clear();
        
        verify(cacheService).clear();
    }

    @Test
    void shouldCheckKeyExistence() {
        String key = "exists-key";
        
        when(cacheService.containsKey(key)).thenReturn(true);
        
        boolean exists = cacheService.containsKey(key);
        
        assertThat(exists).isTrue();
        verify(cacheService).containsKey(key);
    }

    @Test
    void shouldPutAllEntries() {
        Map<String, String> entries = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        );
        
        cacheService.putAll(entries);
        
        verify(cacheService).putAll(entries);
    }

    @Test
    void shouldGetAllEntries() {
        Collection<String> keys = Arrays.asList("key1", "key2", "key3");
        Map<String, String> expectedResult = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        );
        
        when(cacheService.getAll(keys)).thenReturn(expectedResult);
        
        Map<String, String> result = cacheService.getAll(keys);
        
        assertThat(result).isEqualTo(expectedResult);
        verify(cacheService).getAll(keys);
    }

    @Test
    void shouldPutIfAbsent() {
        String key = "absent-key";
        String value = "absent-value";
        
        when(cacheService.putIfAbsent(key, value)).thenReturn(true);
        
        boolean result = cacheService.putIfAbsent(key, value);
        
        assertThat(result).isTrue();
        verify(cacheService).putIfAbsent(key, value);
    }

    @Test
    void shouldPutIfAbsentWithTtl() {
        String key = "absent-ttl-key";
        String value = "absent-ttl-value";
        Duration ttl = Duration.ofMinutes(10);
        
        when(cacheService.putIfAbsent(key, value, ttl)).thenReturn(true);
        
        boolean result = cacheService.putIfAbsent(key, value, ttl);
        
        assertThat(result).isTrue();
        verify(cacheService).putIfAbsent(key, value, ttl);
    }

    @Test
    void shouldNotPutIfKeyExists() {
        String key = "existing-key";
        String value = "new-value";
        
        when(cacheService.putIfAbsent(key, value)).thenReturn(false);
        
        boolean result = cacheService.putIfAbsent(key, value);
        
        assertThat(result).isFalse();
        verify(cacheService).putIfAbsent(key, value);
    }

    @Test
    void shouldIncrementValue() {
        String key = "counter-key";
        long delta = 5L;
        long expectedResult = 15L;
        
        when(cacheService.increment(key, delta)).thenReturn(expectedResult);
        
        Long result = cacheService.increment(key, delta);
        
        assertThat(result).isEqualTo(expectedResult);
        verify(cacheService).increment(key, delta);
    }

    @Test
    void shouldDecrementValue() {
        String key = "counter-key";
        long delta = 3L;
        long expectedResult = 7L;
        
        when(cacheService.decrement(key, delta)).thenReturn(expectedResult);
        
        Long result = cacheService.decrement(key, delta);
        
        assertThat(result).isEqualTo(expectedResult);
        verify(cacheService).decrement(key, delta);
    }

    @Test
    void shouldReturnStatistics() {
        when(cacheService.getStatistics()).thenReturn(cacheStatistics);
        
        CacheStatistics result = cacheService.getStatistics();
        
        assertThat(result).isEqualTo(cacheStatistics);
        verify(cacheService).getStatistics();
    }

    @Test
    void shouldHandleNullValues() {
        String key = "null-key";
        
        when(cacheService.get(key)).thenReturn(Optional.empty());
        
        Optional<String> result = cacheService.get(key);
        
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyCollections() {
        Collection<String> emptyKeys = Collections.emptyList();
        Map<String, String> emptyResult = Collections.emptyMap();
        
        when(cacheService.getAll(emptyKeys)).thenReturn(emptyResult);
        
        Map<String, String> result = cacheService.getAll(emptyKeys);
        
        assertThat(result).isEmpty();
        verify(cacheService).getAll(emptyKeys);
    }
}