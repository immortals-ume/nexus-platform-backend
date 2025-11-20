# Multi-Level Cache Provider

A two-tier caching implementation combining local (L1) and distributed (L2) caches with distributed eviction support and automatic fallback.

## Features

### 1. Two-Tier Caching Strategy
- **L1 Cache**: Fast local in-memory cache (e.g., Caffeine)
- **L2 Cache**: Distributed cache (e.g., Redis)

### 2. Read-Through Logic
1. Check L1 cache first
2. On L1 miss, check L2 cache
3. On L2 hit, populate L1 cache automatically
4. Return value or empty

### 3. Write-Through Logic
- Writes to both L1 and L2 simultaneously
- Ensures consistency across cache levels

### 4. Distributed Eviction Notification
- Uses Redis Pub/Sub for eviction events
- Notifies other application instances when cache entries are invalidated
- Instance-aware to avoid redundant self-evictions
- Supports:
  - Single key eviction
  - Pattern-based eviction
  - Clear all operations

### 5. Automatic Fallback
- Detects L2 (Redis) failures automatically
- Falls back to L1-only operation when L2 is unavailable
- Logs fallback events for monitoring
- Continues serving from L1 cache during L2 outages

## Components

### MultiLevelCacheService
Main implementation coordinating L1 and L2 caches with fallback logic.

### EvictionPublisher / RedisEvictionPublisher
Publishes cache eviction events to Redis Pub/Sub channels.

### EvictionSubscriber
Listens for eviction events and invalidates L1 cache accordingly.

### EvictionEvent
Event model for cache invalidation operations.

## How It Works

### 1. Dependency Injection
The `MultiLevelCacheService` receives L1 and L2 caches via constructor injection:

```java
public MultiLevelCacheService(
    CacheService<K, V> l1Cache,      // Injected - L1CacheService
    CacheService<K, V> l2Cache,      // Injected - RedisCacheService
    EvictionPublisher evictionPublisher,
    String namespace)
```

### 2. Reusing Existing Implementations
**YES!** We directly reuse the existing cache implementations:
- **L1**: `CaffeineCacheService` (already implements `CacheService<K, V>`)
- **L2**: `RedisCacheService` (already implements `CacheService<K, V>`)

No wrapper classes needed - they work out of the box!

### 3. Spring Auto-Configuration
The `MultiLevelCacheAutoConfiguration` automatically wires everything:

```java
@Bean
@Qualifier("l1Cache")
public CacheService<K, V> l1CacheService(...) {
    return new CaffeineCacheService<>(...);  // Existing implementation
}

@Bean
@Qualifier("l2Cache")
public CacheService<K, V> l2CacheService(...) {
    return new RedisCacheService<>(...);  // Existing implementation
}

@Bean
public MultiLevelCacheService<K, V> multiLevelCacheService(
        @Qualifier("l1Cache") CacheService<K, V> l1Cache,
        @Qualifier("l2Cache") CacheService<K, V> l2Cache,
        ...) {
    return new MultiLevelCacheService<>(l1Cache, l2Cache, ...);
}
```

## Configuration

```yaml
# Enable multi-level caching
cache:
  multilevel:
    enabled: true
    eviction-enabled: true
    fallback-enabled: true
    log-fallbacks: true
  
  # L1 (Caffeine) configuration
  caffeine:
    maximum-size: 10000
    ttl: 5m
  
  # L2 (Redis) configuration
  redis:
    time-to-live: 1h
    pipelining:
      enabled: true
      batch-size: 100
```

## Usage Example

### Automatic (Recommended)
Just enable it and inject the auto-configured bean:

```java
@Service
public class UserService {
    
    @Autowired
    private MultiLevelCacheService<String, User> multiLevelCache;
    
    public User getUser(String id) {
        return multiLevelCache.get(id)
            .orElseGet(() -> loadFromDatabase(id));
    }
}
```

### Manual Configuration
For custom namespaces or multiple cache instances:

```java
@Configuration
public class CustomCacheConfig {
    
    @Bean
    public MultiLevelCacheService<String, User> userCache(
            @Qualifier("l1Cache") CacheService<String, User> l1Cache,
            @Qualifier("l2Cache") CacheService<String, User> l2Cache,
            EvictionPublisher evictionPublisher) {
        return new MultiLevelCacheService<>(
            l1Cache,      // Reuses existing L1CacheService
            l2Cache,      // Reuses existing RedisCacheService
            evictionPublisher,
            "users"       // Custom namespace
        );
    }
}
```

## Statistics

The multi-level cache tracks:
- L1 hits/misses
- L2 hits/misses
- L2 failure count
- Fallback operation count

Access statistics via:
```java
multiLevelCache.getL1Statistics();
multiLevelCache.getL2Statistics();
multiLevelCache.getL2FailureCount();
multiLevelCache.getFallbackCount();
```

## Requirements Satisfied

- **1.1**: Multi-level caching with L1 and L2 coordination
- **5.1**: Distributed eviction notification system
- **5.2**: Fallback logic for L2 failures
