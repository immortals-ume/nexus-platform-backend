package com.immortals.platform.cache.features;

import com.immortals.platform.cache.features.compression.CompressionDecorator;
import com.immortals.platform.cache.features.compression.GzipCompressionStrategy;
import com.immortals.platform.cache.features.serialization.JacksonSerializationStrategy;
import com.immortals.platform.cache.providers.caffeine.L1CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cache features functionality.
 * Tests that compression, serialization, and other features work correctly.
 */
class CacheFeaturesIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    com.immortals.platform.cache.config.CacheAutoConfiguration.class,
                    com.immortals.platform.cache.config.CaffeineAutoConfiguration.class,
                    com.immortals.platform.cache.config.DecoratorAutoConfiguration.class
            ));

    @Test
    void shouldWorkWithCompressionEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.features.compression.enabled=true",
                        "platform.cache.features.compression.algorithm=gzip"
                )
                .run(context -> {
                    // Test compression strategy is available
                    assertThat(context).hasSingleBean(GzipCompressionStrategy.class);
                    
                    // Test that cache service works with compression
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    String key = "compression-test";
                    String largeValue = "This is a large string that should benefit from compression. ".repeat(100);
                    
                    cacheService.put(key, largeValue);
                    Optional<String> retrieved = cacheService.get(key);
                    
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get()).isEqualTo(largeValue);
                });
    }

    @Test
    void shouldWorkWithSerializationStrategy() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.features.serialization.strategy=jackson"
                )
                .run(context -> {
                    // Test serialization strategy is available
                    assertThat(context).hasSingleBean(JacksonSerializationStrategy.class);
                    
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, Object> cacheService = (L1CacheService<String, Object>) context.getBean(L1CacheService.class);
                    
                    // Test with complex object
                    Map<String, Object> complexObject = new HashMap<>();
                    complexObject.put("name", "test");
                    complexObject.put("value", 42);
                    complexObject.put("active", true);
                    
                    String key = "serialization-test";
                    cacheService.put(key, complexObject);
                    
                    Optional<Object> retrieved = cacheService.get(key);
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get()).isInstanceOf(Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> retrievedMap = (Map<String, Object>) retrieved.get();
                    assertThat(retrievedMap).containsEntry("name", "test");
                    assertThat(retrievedMap).containsEntry("value", 42);
                    assertThat(retrievedMap).containsEntry("active", true);
                });
    }

    @Test
    void shouldWorkWithMultipleFeaturesCombined() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.features.compression.enabled=true",
                        "platform.cache.features.compression.algorithm=gzip",
                        "platform.cache.features.serialization.strategy=jackson"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, Object> cacheService = (L1CacheService<String, Object>) context.getBean(L1CacheService.class);
                    
                    // Test with complex object and compression
                    Map<String, String> largeObject = new HashMap<>();
                    for (int i = 0; i < 100; i++) {
                        largeObject.put("key" + i, "This is a long value that should compress well " + i);
                    }
                    
                    String key = "combined-features-test";
                    cacheService.put(key, largeObject);
                    
                    Optional<Object> retrieved = cacheService.get(key);
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get()).isInstanceOf(Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, String> retrievedMap = (Map<String, String>) retrieved.get();
                    assertThat(retrievedMap).hasSize(100);
                    assertThat(retrievedMap).containsKey("key0");
                    assertThat(retrievedMap).containsKey("key99");
                });
    }

    @Test
    void shouldHandleFeatureDisabling() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.features.compression.enabled=false",
                        "platform.cache.features.encryption.enabled=false"
                )
                .run(context -> {
                    // Should not have compression or encryption beans
                    assertThat(context).doesNotHaveBean(CompressionDecorator.class);
                    
                    // But cache should still work
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    String key = "no-features-test";
                    String value = "simple-value";
                    
                    cacheService.put(key, value);
                    Optional<String> retrieved = cacheService.get(key);
                    
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get()).isEqualTo(value);
                });
    }

    @Test
    void shouldWorkWithDefaultFeatureConfiguration() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true"
                )
                .run(context -> {
                    // Should work with default feature configuration
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, String> cacheService = (L1CacheService<String, String>) context.getBean(L1CacheService.class);
                    
                    String key = "default-config-test";
                    String value = "test-value";
                    
                    cacheService.put(key, value);
                    Optional<String> retrieved = cacheService.get(key);
                    
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get()).isEqualTo(value);
                });
    }

    @Test
    void shouldHandleComplexDataTypes() {
        contextRunner
                .withPropertyValues(
                        "platform.cache.enabled=true",
                        "platform.cache.caffeine.enabled=true",
                        "platform.cache.features.serialization.strategy=jackson"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    L1CacheService<String, Object> cacheService = (L1CacheService<String, Object>) context.getBean(L1CacheService.class);
                    
                    // Test with nested objects
                    TestComplexObject complexObject = new TestComplexObject();
                    complexObject.setName("test-object");
                    complexObject.setValue(123);
                    
                    TestNestedObject nested = new TestNestedObject();
                    nested.setDescription("nested-description");
                    nested.setActive(true);
                    complexObject.setNested(nested);
                    
                    String key = "complex-object-test";
                    cacheService.put(key, complexObject);
                    
                    Optional<Object> retrieved = cacheService.get(key);
                    assertThat(retrieved).isPresent();
                    assertThat(retrieved.get()).isInstanceOf(TestComplexObject.class);
                    TestComplexObject retrievedObject = (TestComplexObject) retrieved.get();
                    assertThat(retrievedObject.getName()).isEqualTo("test-object");
                    assertThat(retrievedObject.getValue()).isEqualTo(123);
                    assertThat(retrievedObject.getNested()).isNotNull();
                    assertThat(retrievedObject.getNested().getDescription()).isEqualTo("nested-description");
                    assertThat(retrievedObject.getNested().isActive()).isTrue();
                });
    }

    // Test classes for complex object testing
    public static class TestComplexObject {
        private String name;
        private int value;
        private TestNestedObject nested;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public TestNestedObject getNested() { return nested; }
        public void setNested(TestNestedObject nested) { this.nested = nested; }
    }

    public static class TestNestedObject {
        private String description;
        private boolean active;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}