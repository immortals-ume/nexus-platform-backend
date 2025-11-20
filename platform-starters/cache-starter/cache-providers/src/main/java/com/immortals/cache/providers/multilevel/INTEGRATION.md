# Multi-Level Cache Integration Guide

## Answers to Your Questions

### Q1: How does MultiLevelCacheService know which to connect to?

**Answer**: Through **dependency injection** via Spring's `@Qualifier` annotation.

The `MultiLevelCacheService` constructor receives both caches as parameters:

```java
public MultiLevelCacheService(
    CacheService<K, V> l1Cache,      // ← Injected by Spring
    CacheService<K, V> l2Cache,      // ← Injected by Spring
    EvictionPublisher evictionPublisher,
    String namespace)
```

Spring's auto-configuration wires them together:

```java
@Bean
public MultiLevelCacheService<K, V> multiLevelCacheService(
        @Qualifier("l1Cache") CacheService<K, V> l1Cache,    // ← Caffeine
        @Qualifier("l2Cache") CacheService<K, V> l2Cache,    // ← Redis
        EvictionPublisher evictionPublisher) {
    
    return new MultiLevelCacheService<>(l1Cache, l2Cache, evictionPublisher, "default");
}
```

The `@Qualifier` annotations tell Spring:
- `"l1Cache"` → inject the `CaffeineCacheService` bean
- `"l2Cache"` → inject the `RedisCacheService` bean

### Q2: Can we use existing implementations directly?

**Answer**: **YES! Absolutely!** 

Both existing implementations already implement the `CacheService<K, V>` interface:

```java
// Existing Caffeine implementation - works as-is!
public class CaffeineCacheService<K, V> implements CacheService<K, V> { ... }

// Existing Redis implementation - works as-is!
public class RedisCacheService<K, V> implements CacheService<K, V> { ... }
```

**No wrapper classes needed.** The `MultiLevelCacheService` works with any `CacheService` implementation.

## How It All Fits Together

```
┌─────────────────────────────────────────────────────────┐
│         MultiLevelCacheAutoConfiguration                │
│                                                           │
│  Creates and wires:                                      │
│  1. L1 Cache (CaffeineCacheService) ← existing code     │
│  2. L2 Cache (RedisCacheService)    ← existing code     │
│  3. EvictionPublisher (Redis Pub/Sub)                   │
│  4. MultiLevelCacheService (coordinates L1 + L2)        │
│  5. EvictionSubscriber (listens for events)             │
└─────────────────────────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────┐
│            MultiLevelCacheService<K, V>                  │
│                                                           │
│  ┌─────────────┐         ┌─────────────┐                │
│  │  L1 Cache   │         │  L2 Cache   │                │
│  │  (Caffeine) │ ←────→  │  (Redis)    │                │
│  │  Existing!  │         │  Existing!  │                │
│  └─────────────┘         └─────────────┘                │
│         ↑                        ↑                       │
│         │                        │                       │
│         │    Read-through /      │                       │
│         │    Write-through       │                       │
│         │    Fallback logic      │                       │
│         └────────────────────────┘                       │
└─────────────────────────────────────────────────────────┘
                            │
                            ↓
                    Application Code
                    (just inject and use!)
```

## Zero Changes to Existing Code

The existing `CaffeineCacheService` and `RedisCacheService` require **ZERO modifications**:

✅ They already implement `CacheService<K, V>`  
✅ They already have all required methods  
✅ They already handle their own metrics and logging  
✅ They work perfectly as L1 and L2 caches  

## Example Flow

### Read Operation
```
1. App calls: multiLevelCache.get("user:123")
2. MultiLevelCacheService checks L1 (Caffeine)
   └─→ Hit? Return immediately
   └─→ Miss? Continue to step 3
3. MultiLevelCacheService checks L2 (Redis)
   └─→ Hit? Populate L1 and return
   └─→ Miss? Return empty
   └─→ Error? Fallback to L1 only (already checked)
```

### Write Operation
```
1. App calls: multiLevelCache.put("user:123", userData)
2. MultiLevelCacheService writes to L1 (Caffeine)
3. MultiLevelCacheService writes to L2 (Redis)
   └─→ Success? Done
   └─→ Error? Log fallback, continue (L1 has data)
4. Publish eviction event to other instances
```

### Eviction Event
```
1. Instance A removes key: multiLevelCache.remove("user:123")
2. MultiLevelCacheService removes from L1 and L2
3. EvictionPublisher sends event to Redis Pub/Sub
4. Instance B's EvictionSubscriber receives event
5. Instance B removes key from its L1 cache
6. All instances now have consistent caches
```

## Configuration Properties

```yaml
cache:
  # Enable multi-level caching
  multilevel:
    enabled: true              # Master switch
    eviction-enabled: true     # Distributed eviction
    fallback-enabled: true     # L2 failure fallback
    log-fallbacks: true        # Log when L2 fails
  
  # L1 configuration (existing)
  caffeine:
    maximum-size: 10000
    ttl: 5m
  
  # L2 configuration (existing)
  redis:
    time-to-live: 1h
    pipelining:
      enabled: true
      batch-size: 100
```

## Summary

✅ **Reuses existing code** - No changes to `CaffeineCacheService` or `RedisCacheService`  
✅ **Dependency injection** - Spring wires L1 and L2 automatically  
✅ **Transparent to apps** - Just inject `MultiLevelCacheService` and use it  
✅ **Fallback built-in** - Handles L2 failures gracefully  
✅ **Distributed eviction** - Keeps all instances in sync  
