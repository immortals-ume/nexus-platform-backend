package com.immortals.platform.cache.providers;

import com.immortals.platform.cache.core.CacheService;
import com.immortals.platform.cache.providers.caffeine.L1CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cache providers functionality.
 * Tests that cache providers work correctly with different configurations.
 */
class CacheProvidersIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    com.immortals.platform.cache.config.CacheAutoConfiguration.class,
                    com.immortals.platform.cache.config.CaffeineAutoConfiguration.class
            ));

    @Test
    void shouldWorkWithCaffeineProvider() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.caffeine.maximum-size=100"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    // Test basic cache operations
                    String key = "test-key";
                    String value = "test-value";
                    
                    // Put and get
                    cacheService.put(key, value);
                    Optional<String> retrieved = cacheService.get(key);
                    
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get()).isEqualTo(value);
                    
                    // Test removal
                    cacheService.remove(key);
                    Optional<String> afterRemoval = cacheService.get(key);
                    assertThat(afterRemoval).isEmpty();
                });
    }

    @Test
    void shouldWorkWithTtlConfiguration() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.caffeine.ttl=PT1S"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    String key = "ttl-test-key";
                    String value = "ttl-test-value";
                    
                    // Put value
                    cacheService.put(key, value);
                    
                    // Should be present immediately
                    Optional<String> immediate = cacheService.get(key);
                    assertThat(immediate).isPresent();
                    
                    // Wait for TTL to expire (1 second + buffer)
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Should be expired
                    Optional<String> afterTtl = cacheService.get(key);
                    assertThat(afterTtl).isEmpty();
                });
    }

    @Test
    void shouldWorkWithMaximumSizeConfiguration() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.caffeine.maximum-size=2"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    // Add more items than maximum size
                    cacheService.put("key1", "value1");
                    cacheService.put("key2", "value2");
                    cacheService.put("key3", "value3"); // Should evict one of the previous
                    
                    // At least one should be present, but not all three due to size limit
                    int presentCount = 0;
                    if (cacheService.get("key1").isPresent()) presentCount++;
                    if (cacheService.get("key2").isPresent()) presentCount++;
                    if (cacheService.get("key3").isPresent()) presentCount++;
                    
                    assertThat(presentCount).isLessThanOrEqualTo(2);
                });
    }

    @Test
    void shouldHandleNullValues() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    String key = "null-test-key";
                    
                    // Test getting non-existent key
                    Optional<String> nonExistent = cacheService.get(key);
                    assertThat(nonExistent).isEmpty();
                    
                    // Test putting null value (should handle gracefully)
                    cacheService.put(key, null);
                    Optional<String> afterNull = cacheService.get(key);
                    assertThat(afterNull).isEmpty();
                });
    }

    @Test
    void shouldWorkWithDifferentValueTypes() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, Object> cacheService = (L1CacheService<String, Object>) context.getBean(L1CacheService.class);
                    
                    // Test String
                    cacheService.put("string-key", "string-value");
                    Optional<Object> stringValue = cacheService.get("string-key");
                    assertThat(stringValue).isPresent().contains("string-value");
                    
                    // Test Integer
                    cacheService.put("int-key", 42);
                    Optional<Object> intValue = cacheService.get("int-key");
                    assertThat(intValue).isPresent().contains(42);
                    
                    // Test Boolean
                    cacheService.put("bool-key", true);
                    Optional<Object> boolValue = cacheService.get("bool-key");
                    assertThat(boolValue).isPresent().contains(true);
                });
    }
}