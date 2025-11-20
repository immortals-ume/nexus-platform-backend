# Design Document: Cache Service Spring Boot Starter

## Overview

This design transforms the existing cache-service into a production-ready Spring Boot starter module that can be imported into any Spring Boot application. The refactored architecture follows Spring Boot auto-configuration conventions, provides clear module boundaries, and maintains all existing functionality while significantly improving maintainability and usability.

### Design Goals

1. **Zero-code integration** - Add dependency + configure properties = working cache
2. **Modular architecture** - Clear separation of concerns with pluggable components
3. **Backward compatibility** - Preserve existing functionality during migration
4. **Production-ready** - Comprehensive observability, resilience, and security features
5. **Developer-friendly** - Intuitive APIs, excellent documentation, and IDE support

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Application                        │
│  (Imports immortals-cache-spring-boot-starter)              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Auto-Configuration Layer                        │
│  • CacheAutoConfiguration                                    │
│  • Conditional bean creation based on properties/classpath   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Facade Layer                                │
│  • UnifiedCacheManager (single entry point)                  │
│  • Annotation support (@Cacheable, @CachePut, @CacheEvict)  │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        ▼            ▼             ▼
┌──────────┐  ┌──────────┐  ┌──────────────┐
│   Core   │  │ Features │  │ Observability│
│  Module  │  │  Module  │  │    Module    │
└──────────┘  └──────────┘  └──────────────┘
      │             │               │
      ▼             ▼               ▼
┌──────────────────────────────────────────┐
│        Implementation Modules             │
│  • Redis  • Caffeine  • Multi-Level      │
└──────────────────────────────────────────┘
```

### Module Structure


```
immortals-cache-spring-boot-starter/
├── cache-core/                          # Core interfaces and abstractions
│   ├── CacheService<K,V>               # Main cache interface
│   ├── CacheManager                     # Manages multiple cache instances
│   └── CacheStatistics                  # Metrics and statistics
│
├── cache-providers/                     # Cache implementation modules
│   ├── cache-caffeine/                 # Caffeine (L1) implementation
│   ├── cache-redis/                    # Redis (L2) implementation
│   └── cache-multilevel/               # Multi-level cache coordinator
│
├── cache-features/                      # Cross-cutting features
│   ├── compression/                    # Compression utilities
│   ├── encryption/                     # Encryption utilities
│   ├── serialization/                  # Serialization strategies
│   └── eviction/                       # Eviction strategies
│
├── cache-resilience/                    # Resilience patterns
│   ├── CircuitBreakerCache             # Circuit breaker wrapper
│   ├── FallbackCache                   # Fallback strategies
│   └── StampedeProtection              # Cache stampede prevention
│
├── cache-observability/                 # Monitoring and metrics
│   ├── CacheMetrics                    # Micrometer metrics
│   ├── CacheHealthIndicator            # Health checks
│   └── CacheTracing                    # OpenTelemetry tracing
│
├── cache-annotations/                   # Annotation support
│   ├── @Cacheable                      # Read-through caching
│   ├── @CachePut                       # Write-through caching
│   ├── @CacheEvict                     # Cache invalidation
│   └── CacheAspect                     # AOP implementation
│
└── cache-autoconfigure/                 # Spring Boot auto-configuration
    ├── CacheAutoConfiguration          # Main auto-config
    ├── CacheProperties                 # Configuration properties
    └── spring.factories                # Auto-config registration
```

## Components and Interfaces

### 1. Core Module

#### CacheService Interface (Simplified)

```java
package com.immortals.cache.core;

import java.time.Duration;
import java.util.Optional;
import java.util.Map;
import java.util.Collection;

/**
 * Core cache service interface providing unified caching operations.
 * All cache implementations must implement this interface.
 */
public interface CacheService<K, V> {
    
    // Basic operations
    void put(K key, V value);
    void put(K key, V value, Duration ttl);
    Optional<V> get(K key);
    void remove(K key);
    void clear();
    boolean containsKey(K key);
    
    // Batch operations
    void putAll(Map<K, V> entries);
    Map<K, V> getAll(Collection<K> keys);
    
    // Conditional operations
    boolean putIfAbsent(K key, V value);
    boolean putIfAbsent(K key, V value, Duration ttl);
    
    // Atomic operations (Redis only)
    Long increment(K key, long delta);
    Long decrement(K key, long delta);
    
    // Statistics
    CacheStatistics getStatistics();
}
```

#### UnifiedCacheManager

```java
package com.immortals.cache.core;

/**
 * Central facade for managing multiple cache instances.
 * Provides namespace isolation and configuration management.
 */
public interface UnifiedCacheManager {
    
    /**
     * Get or create a cache instance for the given namespace.
     */
    <K, V> CacheService<K, V> getCache(String namespace);
    
    /**
     * Get cache with specific configuration.
     */
    <K, V> CacheService<K, V> getCache(String namespace, CacheConfiguration config);
    
    /**
     * Remove a cache namespace.
     */
    void removeCache(String namespace);
    
    /**
     * Get all cache namespaces.
     */
    Collection<String> getCacheNames();
    
    /**
     * Get aggregated statistics across all caches.
     */
    Map<String, CacheStatistics> getAllStatistics();
}
```

### 2. Provider Implementations

#### Caffeine Provider (L1 Cache)

```java
package com.immortals.cache.provider.caffeine;

/**
 * High-performance local cache using Caffeine.
 * Suitable for single-instance applications or as L1 in multi-level setup.
 */
public class CaffeineCacheService<K, V> implements CacheService<K, V> {
    
    private final Cache<K, V> cache;
    private final CacheMetrics metrics;
    
    // Implementation with Caffeine-specific optimizations
    // - Automatic eviction based on size/time
    // - Async loading support
    // - Weak/soft reference support
}
```

#### Redis Provider (L2 Cache)

```java
package com.immortals.cache.provider.redis;

/**
 * Distributed cache using Redis.
 * Supports clustering, replication, and advanced Redis features.
 */
public class RedisCacheService<K, V> implements CacheService<K, V> {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SerializationStrategy serializer;
    private final CacheMetrics metrics;
    
    // Implementation with Redis-specific features
    // - Pipelining for batch operations
    // - Pub/sub for distributed eviction
    // - Lua scripts for atomic operations
}
```

#### Multi-Level Provider

```java
package com.immortals.cache.provider.multilevel;

/**
 * Combines L1 (Caffeine) and L2 (Redis) for optimal performance.
 * Handles synchronization and fallback strategies.
 */
public class MultiLevelCacheService<K, V> implements CacheService<K, V> {
    
    private final CacheService<K, V> l1Cache;  // Caffeine
    private final CacheService<K, V> l2Cache;  // Redis
    private final EvictionPublisher evictionPublisher;
    
    @Override
    public Optional<V> get(K key) {
        // Try L1 first
        Optional<V> value = l1Cache.get(key);
        if (value.isPresent()) {
            return value;
        }
        
        // Try L2
        value = l2Cache.get(key);
        if (value.isPresent()) {
            // Populate L1
            l1Cache.put(key, value.get());
        }
        
        return value;
    }
    
    // Write-through strategy for puts
    // Distributed eviction notifications
}
```

### 3. Feature Modules

#### Compression

```java
package com.immortals.cache.feature.compression;

public interface CompressionStrategy {
    byte[] compress(byte[] data);
    byte[] decompress(byte[] data);
    String getAlgorithm();
}

public class GzipCompressionStrategy implements CompressionStrategy {
    // GZIP implementation
}

public class CompressionDecorator<K, V> implements CacheService<K, V> {
    private final CacheService<K, V> delegate;
    private final CompressionStrategy compression;
    private final int threshold; // Compress only if size > threshold
    
    // Transparent compression/decompression
}
```

#### Encryption

```java
package com.immortals.cache.feature.encryption;

public interface EncryptionStrategy {
    byte[] encrypt(byte[] data);
    byte[] decrypt(byte[] data);
    String getAlgorithm();
}

public class AesGcmEncryptionStrategy implements EncryptionStrategy {
    // AES-GCM implementation with proper key management
}

public class EncryptionDecorator<K, V> implements CacheService<K, V> {
    private final CacheService<K, V> delegate;
    private final EncryptionStrategy encryption;
    
    // Transparent encryption/decryption
}
```

### 4. Resilience Module

#### Circuit Breaker Integration

```java
package com.immortals.cache.resilience;

public class CircuitBreakerCacheDecorator<K, V> implements CacheService<K, V> {
    
    private final CacheService<K, V> delegate;
    private final CircuitBreaker circuitBreaker;
    private final CacheService<K, V> fallbackCache; // Optional L1 fallback
    
    @Override
    public Optional<V> get(K key) {
        try {
            return circuitBreaker.executeSupplier(() -> delegate.get(key));
        } catch (Exception e) {
            if (fallbackCache != null) {
                return fallbackCache.get(key);
            }
            throw e;
        }
    }
}
```

#### Stampede Protection

```java
package com.immortals.cache.resilience;

public class StampedeProtectionDecorator<K, V> implements CacheService<K, V> {
    
    private final CacheService<K, V> delegate;
    private final RedissonClient redisson;
    private final Duration lockTimeout;
    
    public Optional<V> getWithLoader(K key, Supplier<V> loader) {
        Optional<V> cached = delegate.get(key);
        if (cached.isPresent()) {
            return cached;
        }
        
        RLock lock = redisson.getLock("cache:lock:" + key);
        try {
            if (lock.tryLock(lockTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                try {
                    // Double-check
                    cached = delegate.get(key);
                    if (cached.isPresent()) {
                        return cached;
                    }
                    
                    // Load and cache
                    V value = loader.get();
                    delegate.put(key, value);
                    return Optional.of(value);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return Optional.empty();
    }
}
```

### 5. Observability Module

#### Metrics

```java
package com.immortals.cache.observability;

public class CacheMetrics {
    
    private final MeterRegistry registry;
    private final String cacheName;
    
    private final Counter hits;
    private final Counter misses;
    private final Counter evictions;
    private final Timer getTimer;
    private final Timer putTimer;
    
    public void recordHit() {
        hits.increment();
    }
    
    public void recordMiss() {
        misses.increment();
    }
    
    public double getHitRate() {
        long totalHits = (long) hits.count();
        long totalMisses = (long) misses.count();
        long total = totalHits + totalMisses;
        return total == 0 ? 0.0 : (double) totalHits / total;
    }
}
```

#### Health Indicator

```java
package com.immortals.cache.observability;

@Component
public class CacheHealthIndicator implements HealthIndicator {
    
    private final UnifiedCacheManager cacheManager;
    
    @Override
    public Health health() {
        try {
            Map<String, CacheStatistics> stats = cacheManager.getAllStatistics();
            
            Health.Builder builder = Health.up();
            stats.forEach((name, stat) -> {
                builder.withDetail(name, Map.of(
                    "hitRate", stat.getHitRate(),
                    "size", stat.getSize(),
                    "evictions", stat.getEvictionCount()
                ));
            });
            
            return builder.build();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

### 6. Annotation Support

#### Annotations

```java
package com.immortals.cache.annotation;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
    String namespace() default "default";
    String key() default "";  // SpEL expression
    String condition() default "";  // SpEL condition
    String unless() default "";  // SpEL unless condition
    String ttl() default "1h";  // Human-readable: 1h, 30m, 60s
    boolean compress() default false;
    int compressionThreshold() default 1024;
    boolean encrypt() default false;
    boolean stampedeProtection() default false;
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut {
    String namespace() default "default";
    String key() default "";
    String condition() default "";
    String unless() default "";
    String ttl() default "1h";
    boolean compress() default false;
    boolean encrypt() default false;
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    String namespace() default "default";
    String key() default "";
    String condition() default "";
    boolean allEntries() default false;
    boolean beforeInvocation() default false;
}
```

#### Aspect Implementation

```java
package com.immortals.cache.annotation;

@Aspect
@Component
public class CacheAspect {
    
    private final UnifiedCacheManager cacheManager;
    private final KeyGenerator keyGenerator;
    private final ExpressionEvaluator expressionEvaluator;
    
    @Around("@annotation(cacheable)")
    public Object handleCacheable(ProceedingJoinPoint pjp, Cacheable cacheable) {
        // Simplified, focused implementation
        // - Evaluate condition
        // - Generate key
        // - Check cache
        // - Execute method if miss
        // - Store result
        // - Apply features (compression, encryption) via decorators
    }
}
```

### 7. Auto-Configuration

#### Main Auto-Configuration Class

```java
package com.immortals.cache.autoconfigure;

@AutoConfiguration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public UnifiedCacheManager cacheManager(
            CacheProperties properties,
            ObjectProvider<CacheService<?, ?>> cacheProviders) {
        
        return new DefaultUnifiedCacheManager(properties, cacheProviders);
    }
    
    @Configuration
    @ConditionalOnProperty(name = "immortals.cache.type", havingValue = "caffeine")
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
    static class CaffeineConfiguration {
        
        @Bean
        public CacheService<?, ?> caffeineCacheService(CacheProperties properties) {
            return new CaffeineCacheService<>(properties.getCaffeine());
        }
    }
    
    @Configuration
    @ConditionalOnProperty(name = "immortals.cache.type", havingValue = "redis")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
    static class RedisConfiguration {
        
        @Bean
        public RedisConnectionFactory redisConnectionFactory(CacheProperties properties) {
            // Configure Lettuce with all the SSL, ACL, replica settings
        }
        
        @Bean
        public CacheService<?, ?> redisCacheService(
                RedisTemplate<String, Object> redisTemplate,
                CacheProperties properties) {
            return new RedisCacheService<>(redisTemplate, properties.getRedis());
        }
    }
    
    @Configuration
    @ConditionalOnProperty(name = "immortals.cache.type", havingValue = "multi-level")
    static class MultiLevelConfiguration {
        
        @Bean
        public CacheService<?, ?> multiLevelCacheService(
                @Qualifier("l1Cache") CacheService<?, ?> l1,
                @Qualifier("l2Cache") CacheService<?, ?> l2,
                CacheProperties properties) {
            return new MultiLevelCacheService<>(l1, l2, properties.getMultiLevel());
        }
    }
    
    @Bean
    @ConditionalOnProperty(name = "immortals.cache.resilience.circuit-breaker.enabled", havingValue = "true")
    public BeanPostProcessor circuitBreakerPostProcessor(CircuitBreakerRegistry registry) {
        return new CircuitBreakerCachePostProcessor(registry);
    }
    
    @Bean
    @ConditionalOnProperty(name = "immortals.cache.features.compression.enabled", havingValue = "true")
    public BeanPostProcessor compressionPostProcessor(CacheProperties properties) {
        return new CompressionCachePostProcessor(properties.getFeatures().getCompression());
    }
    
    @Bean
    @ConditionalOnProperty(name = "immortals.cache.features.encryption.enabled", havingValue = "true")
    public BeanPostProcessor encryptionPostProcessor(CacheProperties properties) {
        return new EncryptionCachePostProcessor(properties.getFeatures().getEncryption());
    }
}
```

#### Configuration Properties

```java
package com.immortals.cache.autoconfigure;

@ConfigurationProperties(prefix = "immortals.cache")
public class CacheProperties {
    
    /**
     * Cache type: caffeine, redis, multi-level
     */
    private CacheType type = CacheType.CAFFEINE;
    
    /**
     * Default TTL for cache entries
     */
    private Duration defaultTtl = Duration.ofHours(1);
    
    /**
     * Caffeine-specific configuration
     */
    private CaffeineProperties caffeine = new CaffeineProperties();
    
    /**
     * Redis-specific configuration
     */
    private RedisProperties redis = new RedisProperties();
    
    /**
     * Multi-level cache configuration
     */
    private MultiLevelProperties multiLevel = new MultiLevelProperties();
    
    /**
     * Feature toggles
     */
    private FeatureProperties features = new FeatureProperties();
    
    /**
     * Resilience configuration
     */
    private ResilienceProperties resilience = new ResilienceProperties();
    
    /**
     * Observability configuration
     */
    private ObservabilityProperties observability = new ObservabilityProperties();
    
    // Getters and setters
    
    public static class CaffeineProperties {
        private long maximumSize = 10000;
        private Duration expireAfterWrite = Duration.ofHours(1);
        private Duration expireAfterAccess;
        private boolean recordStats = true;
    }
    
    public static class RedisProperties {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private Duration commandTimeout = Duration.ofSeconds(5);
        private boolean useSsl = false;
        private SslProperties ssl = new SslProperties();
        private AclProperties acl = new AclProperties();
        private PipeliningProperties pipelining = new PipeliningProperties();
    }
    
    public static class FeatureProperties {
        private CompressionProperties compression = new CompressionProperties();
        private EncryptionProperties encryption = new EncryptionProperties();
    }
    
    public static class ResilienceProperties {
        private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();
        private boolean stampedeProtection = false;
        private Duration stampedeTimeout = Duration.ofSeconds(5);
    }
}
```

## Data Models

### CacheConfiguration

```java
public class CacheConfiguration {
    private Duration ttl;
    private EvictionPolicy evictionPolicy;
    private boolean compressionEnabled;
    private boolean encryptionEnabled;
    private Map<String, Object> providerSpecificConfig;
}
```

### CacheStatistics

```java
public class CacheStatistics {
    private long hitCount;
    private long missCount;
    private long evictionCount;
    private long size;
    private double hitRate;
    private Duration averageGetTime;
    private Duration averagePutTime;
}
```

## Error Handling

### Exception Hierarchy

```java
public class CacheException extends RuntimeException {
    // Base exception
}

public class CacheConnectionException extends CacheException {
    // Connection failures
}

public class CacheSerializationException extends CacheException {
    // Serialization errors
}

public class CacheConfigurationException extends CacheException {
    // Configuration errors
}
```

### Error Handling Strategy

1. **Connection Failures**: Circuit breaker opens, fallback to L1 cache
2. **Serialization Errors**: Log error, return empty Optional, don't cache
3. **Configuration Errors**: Fail fast at startup with clear messages
4. **Timeout Errors**: Log warning, return empty Optional, record metric

## Testing Strategy

### Unit Tests

- Test each cache implementation independently
- Mock external dependencies (Redis, Caffeine)
- Test all decorator patterns (compression, encryption, circuit breaker)
- Test annotation processing and key generation

### Integration Tests

- Test with embedded Redis (TestContainers)
- Test multi-level cache synchronization
- Test distributed eviction
- Test circuit breaker behavior under failures

### Performance Tests

- Benchmark different cache providers
- Test compression overhead vs. memory savings
- Test encryption overhead
- Test multi-level cache latency

### Example Test Structure

```java
@SpringBootTest
@Testcontainers
class RedisCacheServiceIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Autowired
    private CacheService<String, String> cacheService;
    
    @Test
    void shouldStoreAndRetrieveValue() {
        cacheService.put("key1", "value1");
        Optional<String> result = cacheService.get("key1");
        assertThat(result).contains("value1");
    }
}
```

## Migration Strategy

### Phase 1: Parallel Implementation

- Keep existing code intact
- Implement new modules alongside
- Add feature flags to switch between old/new

### Phase 2: Gradual Migration

- Migrate one namespace at a time
- Monitor metrics for regressions
- Provide compatibility layer

### Phase 3: Deprecation

- Mark old APIs as @Deprecated
- Provide migration guide
- Remove after 2 major versions

### Backward Compatibility

```java
@Deprecated(since = "2.0", forRemoval = true)
public class LegacyCacheService {
    private final UnifiedCacheManager newManager;
    
    // Delegate to new implementation
    public void put(String key, Object value) {
        newManager.getCache("default").put(key, value);
    }
}
```

## Configuration Examples

### Example 1: Simple In-Memory Cache

```yaml
immortals:
  cache:
    type: caffeine
    default-ttl: 1h
    caffeine:
      maximum-size: 10000
      expire-after-write: 1h
```

### Example 2: Redis with SSL

```yaml
immortals:
  cache:
    type: redis
    default-ttl: 30m
    redis:
      host: redis.example.com
      port: 6380
      password: ${REDIS_PASSWORD}
      use-ssl: true
      ssl:
        trust-store: /path/to/truststore.jks
        trust-store-password: ${TRUSTSTORE_PASSWORD}
```

### Example 3: Multi-Level with All Features

```yaml
immortals:
  cache:
    type: multi-level
    default-ttl: 1h
    
    caffeine:
      maximum-size: 5000
      expire-after-write: 10m
    
    redis:
      host: redis-cluster.example.com
      port: 6379
      pipelining:
        enabled: true
        batch-size: 100
    
    features:
      compression:
        enabled: true
        threshold: 1024
        algorithm: gzip
      encryption:
        enabled: true
        algorithm: AES-GCM
        key: ${ENCRYPTION_KEY}
    
    resilience:
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
      stampede-protection: true
      stampede-timeout: 5s
    
    observability:
      metrics:
        enabled: true
      tracing:
        enabled: true
```

## Deployment Considerations

### Maven Dependency

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>immortals-cache-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Optional Dependencies

```xml
<!-- For Redis support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- For Caffeine support -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- For distributed locking (stampede protection) -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

### Resource Requirements

- **Caffeine**: ~100MB heap per 10K entries (depends on object size)
- **Redis**: Network latency + serialization overhead (~1-5ms per operation)
- **Multi-Level**: L1 memory + L2 network overhead

## Security Considerations

1. **Encryption Keys**: Never hardcode, use environment variables or secret management
2. **SSL/TLS**: Always use in production for Redis connections
3. **ACL**: Use Redis 6+ ACL for fine-grained access control
4. **Sensitive Data**: Consider encryption for PII/sensitive data
5. **Key Naming**: Avoid exposing sensitive information in cache keys

## Performance Optimization

1. **Pipelining**: Enable for batch operations (10-100x throughput improvement)
2. **Compression**: Use for large objects (>1KB) to reduce memory/network
3. **L1 Cache**: Use multi-level for read-heavy workloads (10-100x latency improvement)
4. **Connection Pooling**: Lettuce handles this automatically
5. **Serialization**: Use efficient serializers (Kryo, FST) for complex objects

## Monitoring and Alerting

### Key Metrics to Monitor

- Cache hit rate (target: >80%)
- Average get/put latency (target: <10ms for L1, <50ms for L2)
- Eviction rate (high rate may indicate undersized cache)
- Circuit breaker state (open = degraded service)
- Memory usage (Caffeine heap, Redis memory)

### Recommended Alerts

- Hit rate < 50% for 5 minutes
- Average latency > 100ms for 5 minutes
- Circuit breaker open for > 1 minute
- Redis connection failures > 10 per minute
