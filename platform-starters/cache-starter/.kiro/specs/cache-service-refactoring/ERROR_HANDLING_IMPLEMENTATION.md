# Error Handling Implementation Summary

This document describes how the error handling strategies from task 11.2 are implemented across the cache service modules.

## Requirements (5.2, 5.5)

1. **Handle connection failures with circuit breaker**
2. **Handle serialization errors by logging and returning empty**
3. **Handle configuration errors by failing fast at startup**
4. **Handle timeout errors by logging and recording metrics**

## Implementation Overview

### 1. Connection Failures - Circuit Breaker Pattern

**Location:** `RedisCacheService` (cache-providers module)

**Implementation:**
- Uses Resilience4j `@CircuitBreaker` annotations on Redis operations
- Circuit breaker name: `redisCache`
- Fallback methods for each operation (put, get, remove)

**Fallback Behavior:**
- Records metrics: `cache.circuit_breaker.fallback` counter
- Logs warning with operation details
- For **MultiLevelCacheService**: L1 cache handles the operation automatically
- For **standalone Redis**: Returns empty/silently fails (application continues)

**Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      redisCache:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 100
```

**Example Fallback Method:**
```java
@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getFallback")
public Optional<V> get(K key) {
    // Redis operation
}

private Optional<V> getFallback(K key, Exception e) {
    circuitBreakerFallbacks.incrementAndGet();
    meterRegistry.counter("cache.circuit_breaker.fallback",
            "namespace", namespace,
            "provider", "redis",
            "operation", "get").increment();
    
    log.warn("Circuit breaker fallback for GET key [{}] in namespace [{}]. " +
            "Redis is unavailable. If using multi-level cache, L1 will be checked. " +
            "Returning empty. Error: {}",
            key, namespace, e.getMessage());
    
    return Optional.empty();
}
```

### 2. Serialization Errors

**Location:** 
- `RedisCacheService` - throws `CacheSerializationException`
- Serialization decorators (encryption, compression)

**Implementation:**
- Proper exception hierarchy: `CacheSerializationException` extends `CacheException`
- Includes context: key, operation, value type, direction (serialization/deserialization)
- Detailed error messages with troubleshooting guidance

**Behavior:**
- Log error with full context
- Return empty Optional (don't cache invalid data)
- Record metrics: `cache.errors` counter with type=`serialization_failure`

**Exception Details:**
```java
throw new CacheSerializationException(
    "Failed to serialize value",
    key.toString(),
    CacheOperation.PUT,
    valueType,
    SerializationDirection.SERIALIZATION,
    cause
);
```

### 3. Configuration Errors

**Location:** `DefaultDecoratorChainBuilder` (cache-autoconfigure module)

**Implementation:**
- Validates configuration at startup
- Fails fast with `IllegalStateException` for invalid encryption keys
- Validates required properties in `CacheProperties` with `@Valid` annotations

**Behavior:**
- Application startup fails immediately
- Clear error messages indicating what's wrong
- Troubleshooting guidance in exception message

**Example:**
```java
if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
    log.error("Encryption enabled but no encryption key provided");
    throw new IllegalStateException("Invalid encryption configuration");
}
```

### 4. Timeout Errors

**Location:** 
- Redis connection level: `commandTimeout` configuration
- Optional: `TimeoutCacheDecorator` (cache-resilience module)

**Implementation:**

#### A. Redis Connection Timeout (Primary)
```yaml
immortals:
  cache:
    redis:
      command-timeout: 5s  # Timeout at connection level
```

Configured in `RedisAutoConfiguration`:
```java
clientConfigBuilder.commandTimeout(config.getCommandTimeout());
```

**Behavior:**
- Redis client throws exception on timeout
- Caught by circuit breaker
- Fallback method handles it
- Metrics recorded

#### B. TimeoutCacheDecorator (Optional)
- Wraps cache operations with timeout handling
- Uses `ExecutorService` with `Future.get(timeout)`
- Cancels operation if timeout exceeded

**Behavior:**
- Log warning with operation details
- Record metrics: `cache.timeouts` counter
- Return empty Optional
- Cancel the operation

### 5. Multi-Level Cache Fallback

**Location:** `MultiLevelCacheService` (cache-providers module)

**Implementation:**
- Catches all L2 (Redis) exceptions
- Automatically falls back to L1 (Caffeine)
- Tracks fallback metrics

**Example:**
```java
try {
    Optional<V> value = l2Cache.get(key);
    if (value.isPresent()) {
        populateL1(key, value.get());
        return value;
    }
} catch (Exception e) {
    l2Failures.incrementAndGet();
    fallbackCount.incrementAndGet();
    log.warn("L2 cache failure for key: {}, falling back to L1 only. Error: {}", 
            key, e.getMessage());
    return Optional.empty(); // L1 already checked
}
```

## Exception Hierarchy

```
PlatformException (from platform-common)
└── CacheException
    ├── CacheConnectionException
    │   - Host, port, retryable flag
    │   - Detailed troubleshooting guidance
    │
    ├── CacheSerializationException
    │   - Value type, direction (ser/deser)
    │   - Troubleshooting for serialization issues
    │
    └── CacheConfigurationException
        - Config property, invalid value
        - Configuration examples and validation rules
```

All exceptions include:
- Error code
- Cache key (if applicable)
- Cache operation (GET, PUT, REMOVE, etc.)
- Detailed error messages
- Troubleshooting guidance

## Metrics

All error handling records metrics via Micrometer:

1. **Circuit Breaker Fallbacks:**
   - `cache.circuit_breaker.fallback{namespace, provider, operation}`

2. **Connection Failures:**
   - `cache.errors{type=connection_failure, namespace}`

3. **Serialization Failures:**
   - `cache.errors{type=serialization_failure, namespace}`

4. **Timeout Failures:**
   - `cache.timeouts{namespace}`
   - `cache.errors{type=timeout_failure, namespace}`

5. **Operation Errors:**
   - `cache.put{namespace, provider, status=error}`
   - `cache.get{namespace, provider, status=error}`
   - etc.

## Logging

All errors are logged with:
- Correlation IDs (when available via MDC)
- Namespace context
- Cache key
- Operation type
- Error message and stack trace
- Troubleshooting guidance (for user-facing errors)

**Log Levels:**
- `ERROR`: Unexpected failures, serialization errors
- `WARN`: Circuit breaker fallbacks, timeout errors, L2 failures
- `INFO`: Circuit breaker state transitions
- `DEBUG`: Successful operations, cache hits/misses

## Testing Error Handling

To test error handling:

1. **Connection Failures:**
   - Stop Redis server
   - Verify circuit breaker opens after threshold
   - Verify fallback to L1 in multi-level setup
   - Check metrics: `cache.circuit_breaker.fallback`

2. **Serialization Errors:**
   - Cache object with non-serializable fields
   - Verify error logged with details
   - Verify empty returned (no cache)
   - Check metrics: `cache.errors{type=serialization_failure}`

3. **Configuration Errors:**
   - Set invalid encryption key
   - Verify application fails to start
   - Verify clear error message

4. **Timeout Errors:**
   - Set very low `command-timeout` (e.g., 1ms)
   - Perform cache operations
   - Verify timeout logged
   - Check metrics: `cache.timeouts`

## Summary

The error handling implementation follows the requirements:

✅ **Connection failures** → Circuit breaker with fallback to L1  
✅ **Serialization errors** → Log and return empty  
✅ **Configuration errors** → Fail fast at startup  
✅ **Timeout errors** → Log and record metrics  

All error handling is integrated at the appropriate levels:
- **Provider level**: Circuit breaker annotations, exception throwing
- **Multi-level level**: Automatic L2→L1 fallback
- **Decorator level**: Optional timeout handling
- **Autoconfigure level**: Configuration validation

The implementation is production-ready with comprehensive metrics, logging, and troubleshooting guidance.
