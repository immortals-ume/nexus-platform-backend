# Design Document: Namespace System Redesign

## Overview

The namespace system is redesigned to use a single shared cache instance with NamespacedCacheService handling key prefixing for all namespaces. This eliminates the complexity of creating new instances per namespace and removes the silent failures caused by prototype beans.

## Architecture

```
Application Code
    ↓
CacheAspect (@CachePut, @Cacheable, @CacheEvict)
    ↓
UnifiedCacheManager.getCache(namespace)
    ↓
    ├─ Check cache: cacheInstances.get(namespace)
    │  ├─ If exists: return cached NamespacedCacheService
    │  └─ If not exists: create and cache
    │
    └─ Create NamespacedCacheService
        ├─ Get base cache from factory (singleton)
        ├─ Wrap with NamespacedCacheService(baseCache, namespace)
        ├─ Apply decorator chain
        └─ Cache and return

Base Cache Instance (Singleton)
    ├─ Caffeine L1CacheService
    ├─ Redis RedisCacheService
    └─ Multi-level MultiLevelCacheService
```

## Key Changes

### 1. CacheServiceFactory Returns Singleton

```java
public interface CacheServiceFactory<K, V> {
    CacheService<K, V> createCacheService();
}

// Implementation always returns the same instance
return () -> singletonCacheInstance;
```

### 2. UnifiedCacheManager Caches Wrapped Instances

```java
private final Map<String, CacheService<?, ?>> cacheInstances = new ConcurrentHashMap<>();

public <K, V> CacheService<K, V> getCache(String namespace) {
    return (CacheService<K, V>) cacheInstances.computeIfAbsent(namespace, ns -> {
        // Create NamespacedCacheService wrapping the singleton base cache
        CacheService<String, V> baseCache = baseCacheFactory.createCacheService();
        NamespacedCacheService<K, V> wrapped = new NamespacedCacheService<>(baseCache, namespace);
        return decoratorChainBuilder.buildDecoratorChain(wrapped, namespace, config);
    });
}
```

### 3. No Prototype Beans

All cache services are singleton beans:
- L1CacheService: @Bean (singleton)
- RedisCacheService: @Bean (singleton)
- MultiLevelCacheService: @Bean (singleton)

### 4. Factory Returns Singleton

```java
@Bean
public CacheServiceFactory<?, ?> baseCacheFactory(
        CacheService<String, Object> singletonCacheService) {
    return () -> singletonCacheService;  // Always return the same instance
}
```

## Data Flow

### Storing a Value

```
@CachePut(namespace="users", key="#userId")
public User createUser(String userId, User user) {
    return userRepository.save(user);
}

Flow:
1. CacheAspect intercepts the call
2. Calls cacheManager.getCache("users")
3. UnifiedCacheManager returns NamespacedCacheService("users")
4. CacheAspect calls cache.put("userId", user)
5. NamespacedCacheService prefixes key: "users:userId"
6. Base cache stores: "users:userId" → user
```

### Retrieving a Value

```
@Cacheable(namespace="users", key="#userId")
public User getUser(String userId) {
    return userRepository.findById(userId);
}

Flow:
1. CacheAspect intercepts the call
2. Calls cacheManager.getCache("users")
3. UnifiedCacheManager returns cached NamespacedCacheService("users")
4. CacheAspect calls cache.get("userId")
5. NamespacedCacheService prefixes key: "users:userId"
6. Base cache retrieves: "users:userId" → user
7. Returns user to caller
```

## Components

### 1. CacheServiceFactory

```java
public interface CacheServiceFactory<K, V> {
    CacheService<K, V> createCacheService();
}
```

- Simple factory that returns a cache service instance
- Implementation returns the same singleton instance every time
- No prototype beans, no ObjectProvider complexity

### 2. UnifiedCacheManager

```java
public class DefaultUnifiedCacheManager implements UnifiedCacheManager {
    private final Map<String, CacheService<?, ?>> cacheInstances;
    private final CacheServiceFactory<?, ?> baseCacheFactory;
    
    public <K, V> CacheService<K, V> getCache(String namespace) {
        return (CacheService<K, V>) cacheInstances.computeIfAbsent(namespace, ns -> {
            CacheService<String, V> baseCache = baseCacheFactory.createCacheService();
            NamespacedCacheService<K, V> wrapped = new NamespacedCacheService<>(baseCache, namespace);
            return decoratorChainBuilder.buildDecoratorChain(wrapped, namespace, config);
        });
    }
}
```

- Manages namespace-to-cache mappings
- Caches wrapped instances to avoid recreation
- Creates NamespacedCacheService on-demand for new namespaces

### 3. NamespacedCacheService

```java
public class NamespacedCacheService<K, V> implements CacheService<K, V> {
    private final CacheService<String, V> delegate;
    private final String namespace;
    
    private String buildNamespacedKey(K key) {
        return namespace + ":" + key.toString();
    }
    
    public void put(K key, V value) {
        delegate.put(buildNamespacedKey(key), value);
    }
    
    public Optional<V> get(K key) {
        return delegate.get(buildNamespacedKey(key));
    }
}
```

- Wraps the base cache
- Prefixes all keys with namespace
- Transparent to callers

### 4. CacheAspect

```java
@Aspect
public class CacheAspect {
    private final UnifiedCacheManager cacheManager;
    
    @Around("@annotation(cachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) {
        CacheService<String, Object> cache = cacheManager.getCache(cachePut.namespace());
        // Use cache...
    }
}
```

- Gets cache from UnifiedCacheManager
- Uses the namespace-aware cache service
- Works transparently with key prefixing

## Benefits

1. **Simplicity**: Single shared cache instance, no prototype bean complexity
2. **Reliability**: No silent failures from ObjectProvider
3. **Performance**: Cache instances are created once and reused
4. **Maintainability**: Clear separation of concerns (key prefixing vs. cache management)
5. **Scalability**: Works with any number of namespaces without creating new instances

## Testing Strategy

1. **Unit Tests**: Test NamespacedCacheService key prefixing
2. **Integration Tests**: Test UnifiedCacheManager with multiple namespaces
3. **Functional Tests**: Test @CachePut, @Cacheable, @CacheEvict with namespaces
4. **End-to-End Tests**: Test full flow from annotation to cache storage

## Migration Path

1. Remove @Scope("prototype") from all cache service beans
2. Update CacheServiceFactory to return singleton instances
3. Update AutoConfiguration classes to inject singleton beans
4. Update UnifiedCacheManager to cache wrapped instances
5. Test with existing application code
