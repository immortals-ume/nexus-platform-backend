# Design Document: Fix AutoConfiguration Architecture

## Overview

This design restructures the cache service to follow Spring Boot's autoconfiguration pattern correctly:

- **Configuration Classes** (cache-providers): Create actual infrastructure beans (Caffeine Cache, RedisTemplate, etc.)
- **AutoConfiguration Classes** (cache-features): Orchestrate and wire Configuration beans based on YAML properties
- **Decorator Classes**: Fully implemented to add cross-cutting concerns
- **Properties Flow**: YAML → CacheProperties → Provider Properties → Configuration → Beans

The key principle: **Configuration classes do the work, AutoConfiguration classes do the wiring.**

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    application.yml                              │
│  immortals.cache.type: multi-level                             │
│  immortals.cache.caffeine.maximum-size: 10000                 │
│  immortals.cache.redis.host: localhost                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              CacheAutoConfiguration                             │
│              (@AutoConfiguration)                               │
│  - Imports CaffeineAutoConfiguration                           │
│  - Imports RedisAutoConfiguration                              │
│  - Imports MultiLevelAutoConfiguration                         │
│  - Creates UnifiedCacheManager                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
        ┌─────────────────────────────────────────┐
        │  MultiLevelAutoConfiguration            │
        │  (@Configuration)                       │
        │  - Imports CaffeineAutoConfiguration    │
        │  - Imports RedisAutoConfiguration       │
        │  - Creates Supplier<CacheService>       │
        │    that returns MultiLevelCacheService  │
        └─────────────────────────────────────────┘
                    ↙                    ↖
    ┌──────────────────────┐    ┌──────────────────────┐
    │ CaffeineAutoConfig   │    │ RedisAutoConfig      │
    │ - Creates Supplier   │    │ - Creates Supplier   │
    │   for L1CacheService │    │   for RedisCacheService
    └──────────────────────┘    └──────────────────────┘
            ↓                            ↓
    ┌──────────────────────┐    ┌──────────────────────┐
    │ CaffeineConfiguration│    │ RedisConfiguration   │
    │ (@Configuration)     │    │ (@Configuration)     │
    │ - Creates Caffeine   │    │ - Creates            │
    │   Cache bean         │    │   RedisTemplate      │
    │ - Creates            │    │ - Creates            │
    │   L1CacheService     │    │   RedisCacheService  │
    └──────────────────────┘    └──────────────────────┘
            ↓                            ↓
    ┌──────────────────────┐    ┌──────────────────────┐
    │ Caffeine Cache       │    │ RedisTemplate        │
    │ (actual bean)        │    │ (actual bean)        │
    └──────────────────────┘    └──────────────────────┘
```

## Components and Interfaces

### 1. Configuration Classes (cache-providers module)

#### CaffeineConfiguration
- **Location**: `cache-providers/src/main/java/com/immortals/cache/providers/caffeine/CaffeineConfiguration.java`
- **Responsibility**: Create Caffeine Cache and L1CacheService beans
- **Beans Created**:
  - `Cache<Object, Object> caffeineCache(CaffeineProperties)` - Actual Caffeine cache instance
  - `L1CacheService<String, Object> l1CacheService(Cache, MeterRegistry, String namespace)` - Service wrapper
- **Conditions**: `@ConditionalOnClass(Caffeine.class)` and `@ConditionalOnProperty(name="immortals.cache.type", havingValue="caffeine")`
- **Validation**: Validate maximum size > 0, TTL is valid duration

#### RedisConfiguration
- **Location**: `cache-providers/src/main/java/com/immortals/cache/providers/redis/RedisConfiguration.java`
- **Responsibility**: Create RedisTemplate and RedisCacheService beans
- **Beans Created**:
  - `RedisTemplate<String, Object> redisTemplate(RedisProperties)` - Configured template with serialization
  - `RedisCacheService<String, Object> redisCacheService(RedisTemplate, MeterRegistry, String namespace)` - Service wrapper
- **Conditions**: `@ConditionalOnClass(RedisTemplate.class)` and `@ConditionalOnProperty(name="immortals.cache.type", havingValue="redis")`
- **Validation**: Validate host/port are valid, connection pool settings are positive

#### MultiLevelCacheConfiguration
- **Location**: `cache-providers/src/main/java/com/immortals/cache/providers/multilevel/MultiLevelCacheConfiguration.java`
- **Responsibility**: Create L1 and L2 cache services with eviction publisher
- **Beans Created**:
  - `L1CacheService<String, Object> l1Cache(...)` - Delegates to CaffeineConfiguration
  - `RedisCacheService<String, Object> l2Cache(...)` - Delegates to RedisConfiguration
  - `EvictionPublisher evictionPublisher(RedisTemplate)` - Redis pub/sub for distributed invalidation
  - `MultiLevelCacheService<String, Object> multiLevelCache(L1, L2, Publisher)` - Coordinator
- **Conditions**: `@ConditionalOnProperty(name="immortals.cache.type", havingValue="multi-level")`
- **Validation**: Validate both L1 and L2 are properly initialized

### 2. AutoConfiguration Classes (cache-features module)

#### CacheAutoConfiguration
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/autoconfigure/CacheAutoConfiguration.java`
- **Responsibility**: Main orchestrator, imports all provider configurations
- **Beans Created**:
  - `UnifiedCacheManager unifiedCacheManager(Supplier<CacheService>, CacheProperties)` - Main facade
- **Imports**: CaffeineAutoConfiguration, RedisAutoConfiguration, MultiLevelAutoConfiguration
- **Logic**: Only wiring, no implementation

#### CaffeineAutoConfiguration
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/autoconfigure/CaffeineAutoConfiguration.java`
- **Responsibility**: Orchestrate Caffeine configuration
- **Beans Created**:
  - `CaffeineProperties caffeineProperties(CacheProperties)` - Extract from main properties
  - `Supplier<CacheService<?, ?>> baseCacheProvider(CaffeineConfiguration)` - Delegate to Configuration
- **Imports**: CaffeineConfiguration (from cache-providers)
- **Logic**: Only property extraction and supplier creation

#### RedisAutoConfiguration
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/autoconfigure/RedisAutoConfiguration.java`
- **Responsibility**: Orchestrate Redis configuration
- **Beans Created**:
  - `RedisProperties redisProperties(CacheProperties)` - Extract from main properties
  - `Supplier<CacheService<?, ?>> baseCacheProvider(RedisConfiguration)` - Delegate to Configuration
- **Imports**: RedisConfiguration (from cache-providers)
- **Logic**: Only property extraction and supplier creation

#### MultiLevelAutoConfiguration
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/autoconfigure/MultiLevelAutoConfiguration.java`
- **Responsibility**: Orchestrate multi-level configuration
- **Beans Created**:
  - `Supplier<CacheService<?, ?>> baseCacheProvider(MultiLevelCacheConfiguration)` - Delegate to Configuration
- **Imports**: CaffeineAutoConfiguration, RedisAutoConfiguration, MultiLevelCacheConfiguration
- **Logic**: Only supplier creation

### 3. Decorator Classes (cache-features module)

#### CompressionDecorator
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/compression/CompressionDecorator.java`
- **Responsibility**: Compress values before storage, decompress on retrieval
- **Strategy**: GzipCompressionStrategy
- **Threshold**: Only compress if value size > threshold

#### EncryptionDecorator
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/encryption/EncryptionDecorator.java`
- **Responsibility**: Encrypt values before storage, decrypt on retrieval
- **Strategy**: AesGcmEncryptionStrategy
- **Key Management**: Use provided encryption key

#### MetricsDecorator
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/metrics/MetricsDecorator.java`
- **Responsibility**: Record operation latency, hit/miss rates, throughput
- **Metrics**: Use MeterRegistry to record timers and counters

#### StampedeProtectionDecorator
- **Location**: `cache-features/src/main/java/com/immortals/cache/features/resilience/StampedeProtectionDecorator.java`
- **Responsibility**: Prevent thundering herd using distributed locks
- **Lock Provider**: Redisson for distributed locking
- **Timeout**: Configurable lock timeout

### 4. DefaultDecoratorChainBuilder (Fully Implemented)

- **Location**: `cache-features/src/main/java/com/immortals/cache/features/autoconfigure/DefaultDecoratorChainBuilder.java`
- **Responsibility**: Apply all decorators in correct order
- **Order**: Metrics → Compression → Encryption → StampedeProtection
- **Logic**: Check each decorator's enabled flag and apply if conditions met

## Data Models

### CacheProperties (Main)
```
immortals.cache:
  enabled: true
  type: multi-level  # caffeine, redis, multi-level
  default-ttl: 1h
  features:
    compression:
      enabled: true
      threshold: 1024
    encryption:
      enabled: true
      key: ${ENCRYPTION_KEY}
  resilience:
    stampede-protection:
      enabled: true
      timeout: 30s
    circuit-breaker:
      enabled: true
  caffeine:
    maximum-size: 10000
    ttl: 1h
  redis:
    host: localhost
    port: 6379
    pipelining:
      enabled: true
      batch-size: 100
  multi-level:
    eviction-publisher: redis
```

### Provider-Specific Properties
- **CaffeineProperties**: maximum-size, ttl
- **RedisProperties**: host, port, pipelining settings, connection pool
- **MultiLevelCacheProperties**: eviction-publisher settings

## Error Handling

1. **Configuration Validation**: All Configuration classes validate settings at bean creation time
2. **Fail Fast**: Invalid configuration throws CacheConfigurationException immediately
3. **Clear Messages**: Error messages include property name, constraint, and actual value
4. **Fallback**: MultiLevelCacheService falls back to L1 if L2 fails

## Testing Strategy

1. **Unit Tests**: Test each Configuration class independently
2. **Integration Tests**: Test AutoConfiguration wiring with different property combinations
3. **Decorator Tests**: Test each decorator applies correctly
4. **End-to-End Tests**: Test full flow from YAML to cache operations
5. **Failure Tests**: Test configuration validation and error handling

## Implementation Order

1. Complete CaffeineConfiguration with full bean creation and validation
2. Complete RedisConfiguration with full bean creation and validation
3. Complete MultiLevelCacheConfiguration with L1/L2 coordination
4. Implement all Decorator classes (Compression, Encryption, Metrics, StampedeProtection)
5. Implement DefaultDecoratorChainBuilder to apply decorators
6. Update AutoConfiguration classes to only orchestrate
7. Add comprehensive tests for all components

