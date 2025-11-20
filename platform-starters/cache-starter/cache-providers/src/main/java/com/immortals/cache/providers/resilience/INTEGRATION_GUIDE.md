# Resilience Decorators Integration Guide

## Overview

The resilience package provides a set of decorators that add cross-cutting concerns to cache services:
- **Metrics**: Operation latency and hit/miss tracking
- **Circuit Breaker**: Failure prevention and fallback support
- **Stampede Protection**: Distributed lock-based cache stampede prevention

## Architecture

### Decorator Chain Order (Outermost to Innermost)

```
┌─────────────────────────────────────────┐
│   MetricsDecorator (Outermost)          │
│   - Records all operations              │
│   - Tracks hit/miss rates               │
│   - Measures latency                    │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│   CircuitBreakerDecorator               │
│   - Prevents cascading failures         │
│   - Fallback to L1 cache when open      │
│   - Exposes circuit state via metrics   │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│   StampedeProtectionDecorator           │
│   - Distributed lock serialization      │
│   - Double-check locking pattern        │
│   - Computation timeout protection      │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│   Base Cache Service (Innermost)        │
│   - RedisCacheService                   │
│   - L1CacheService (Caffeine)           │
│   - MultiLevelCacheService              │
└─────────────────────────────────────────┘
```

## Integration Points

### 1. Redis Standalone Cache (CacheStandaloneConfiguration)

```java
@Bean
public CacheService<String, Object> redisCacheService(
        RedisTemplate<String, Object> redisTemplate,
        MeterRegistry meterRegistry,
        RedisProperties redisProperties,
        @Autowired(required = false) RedissonClient redissonClient) {
    
    // Create base cache service
    RedisCacheService<String, Object> baseCache = new RedisCacheService<>(...);
    
    // Apply decorators via DecoratorChainFactory
    DecoratorChainFactory factory = new DecoratorChainFactory(...);
    return factory.buildDecoratorChain(
        baseCache,
        "default",
        enableMetrics = true,
        enableStampedeProtection = redisProperties.getResilience().getStampedeProtection().isEnabled(),
        enableCircuitBreaker = redisProperties.getResilience().getCircuitBreaker().isEnabled(),
        fallbackCache = null
    );
}
```

**Configuration Properties:**
```yaml
immortals:
  cache:
    redis:
      resilience:
        circuit-breaker:
          enabled: true
          failure-rate-threshold: 50
          wait-duration-in-open-state: 60s
        stampede-protection:
          enabled: true
          lock-timeout: 5s
```

### 2. Caffeine L1 Cache (CaffeineConfiguration)

```java
@Bean
public CacheService<String, Object> l1CacheService(
        Cache<Object, Object> caffeineCache,
        CaffeineProperties caffeineProperties,
        MeterRegistry meterRegistry,
        @Autowired(required = false) RedissonClient redissonClient) {
    
    // Create base cache service
    L1CacheService<String, Object> baseService = new L1CacheService<>(...);
    
    // Apply decorators
    DecoratorChainFactory factory = new DecoratorChainFactory(...);
    return factory.buildDecoratorChain(
        baseService,
        "default",
        enableMetrics = true,
        enableStampedeProtection = redissonClient != null,
        enableCircuitBreaker = false,  // Disabled for L1 by default
        fallbackCache = null
    );
}
```

### 3. Multi-Level Cache (MultiLevelCacheAutoConfiguration)

```java
@Bean
public <K, V> CacheService<K, V> multiLevelCacheService(
        @Qualifier("l1Cache") CacheService<K, V> l1Cache,
        @Qualifier("l2Cache") CacheService<K, V> l2Cache,
        EvictionPublisher evictionPublisher,
        MultiLevelCacheProperties properties,
        MeterRegistry meterRegistry,
        @Autowired(required = false) RedissonClient redissonClient) {
    
    // Create base multi-level cache
    MultiLevelCacheService<K, V> baseCache = new MultiLevelCacheService<>(...);
    
    // Apply decorators with L1 as fallback
    DecoratorChainFactory factory = new DecoratorChainFactory(...);
    return factory.buildDecoratorChain(
        baseCache,
        "default",
        enableMetrics = true,
        enableStampedeProtection = redissonClient != null,
        enableCircuitBreaker = true,  // Enabled for multi-level
        fallbackCache = l1Cache       // Fallback to L1 when L2 fails
    );
}
```

## Decorator Details

### MetricsDecorator

**Metrics Recorded:**
- `cache.hits` - Number of cache hits
- `cache.misses` - Number of cache misses
- `cache.puts` - Number of put operations
- `cache.removes` - Number of remove operations
- `cache.evictions` - Number of evictions
- `cache.get.duration` - Get operation latency
- `cache.put.duration` - Put operation latency
- `cache.remove.duration` - Remove operation latency

**Tags:**
- `namespace` - Cache namespace (e.g., "default", "l1", "l2")

### CircuitBreakerDecorator

**Features:**
- Configurable failure rate threshold (0-100%)
- Configurable wait duration before attempting to close
- Automatic fallback to fallback cache when circuit is open
- State inspection via `getCircuitBreakerState()`
- Metrics via Resilience4j integration

**States:**
- `CLOSED` - Normal operation
- `OPEN` - Too many failures, requests rejected
- `HALF_OPEN` - Testing if service recovered

### StampedeProtectionDecorator

**Features:**
- Distributed lock-based serialization using Redisson
- Double-check locking pattern
- Computation timeout protection
- `getWithLoader()` method for safe value computation

**Usage:**
```java
CacheService<String, User> cache = ...;
StampedeProtectionDecorator<String, User> stampede = 
    (StampedeProtectionDecorator<String, User>) cache;

Optional<User> user = stampede.getWithLoader(
    userId,
    id -> loadUserFromDatabase(id),  // Computation function
    Duration.ofHours(1)               // TTL
);
```

## Configuration

### DecoratorChainFactory Constructor

```java
public DecoratorChainFactory(
    MeterRegistry meterRegistry,
    RedissonClient redissonClient,
    Duration stampedeTimeout,           // Lock acquisition timeout
    Duration computationTimeout,        // Value computation timeout
    int circuitBreakerFailureThreshold, // Failure rate %
    Duration circuitBreakerWaitDuration // Wait before half-open
)
```

### Building Decorator Chains

**Full Chain (All Features):**
```java
CacheService<K, V> decorated = factory.buildFullDecoratorChain(
    baseCache,
    namespace,
    fallbackCache
);
```

**Custom Chain:**
```java
CacheService<K, V> decorated = factory.buildDecoratorChain(
    baseCache,
    namespace,
    enableMetrics = true,
    enableStampedeProtection = true,
    enableCircuitBreaker = true,
    fallbackCache = l1Cache
);
```

**Metrics Only:**
```java
CacheService<K, V> decorated = factory.buildMetricsOnlyChain(
    baseCache,
    namespace
);
```

## Monitoring

### Metrics Endpoints

All metrics are exposed via Micrometer and available at:
- `/actuator/metrics` - List all metrics
- `/actuator/metrics/cache.hits` - Cache hit count
- `/actuator/metrics/cache.misses` - Cache miss count
- `/actuator/metrics/cache.get.duration` - Get latency

### Circuit Breaker Metrics

- `resilience4j.circuitbreaker.state` - Current state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j.circuitbreaker.calls` - Total calls
- `resilience4j.circuitbreaker.calls.success` - Successful calls
- `resilience4j.circuitbreaker.calls.failure` - Failed calls

### Stampede Protection Metrics

- `cache.stampede.protection.activated` - Times stampede protection activated
- `cache.stampede.protection.double_check_hit` - Double-check hits
- `cache.stampede.protection.computation_success` - Successful computations
- `cache.stampede.protection.computation_failure` - Failed computations
- `cache.stampede.protection.computation_timeout` - Computation timeouts
- `cache.stampede.lock.wait` - Lock wait time
- `cache.stampede.computation` - Computation time
- `cache.stampede.total` - Total operation time

## Best Practices

1. **Enable Metrics Always** - Provides visibility into cache behavior
2. **Use Circuit Breaker for L2 Cache** - Prevents cascading failures in distributed cache
3. **Enable Stampede Protection** - Prevents thundering herd on cache misses
4. **Configure Appropriate Timeouts** - Balance between responsiveness and reliability
5. **Monitor Metrics** - Set up alerts for high miss rates or circuit breaker trips
6. **Test Fallback Behavior** - Ensure fallback cache works correctly

## Troubleshooting

### Circuit Breaker Always Open

**Symptoms:** All cache operations fail immediately

**Causes:**
- Failure rate threshold too low
- Underlying cache service has persistent issues
- Network connectivity problems

**Solution:**
- Increase failure rate threshold
- Check underlying cache service health
- Verify network connectivity

### High Stampede Protection Activation

**Symptoms:** Many concurrent requests for same key

**Causes:**
- Popular cache keys with long computation time
- Distributed system with many instances

**Solution:**
- Increase TTL to reduce cache misses
- Optimize computation function
- Consider pre-loading popular keys

### Metrics Not Appearing

**Symptoms:** No cache metrics in `/actuator/metrics`

**Causes:**
- MeterRegistry not injected
- Metrics endpoint not enabled
- Actuator not configured

**Solution:**
- Verify MeterRegistry bean exists
- Enable actuator: `management.endpoints.web.exposure.include=metrics`
- Check Spring Boot Actuator configuration
