# Cache Observability Module

This module provides comprehensive observability features for the cache service using an AOP-based approach to avoid code duplication.

## Features

### 1. Comprehensive Metrics Collection (CacheMetrics)
- **Counters**: Tracks hits, misses, evictions, puts, removes, and errors
- **Timers**: Records latency for get, put, remove, getAll, and putAll operations
- **Gauges**: Monitors cache size and memory usage
- **Hit Rate**: Automatically calculates and exposes hit rate as a gauge

All metrics are automatically tagged with:
- `cache`: Cache implementation name
- `namespace`: Cache namespace

### 2. AOP-Based Instrumentation (CacheObservabilityAspect)
The aspect automatically intercepts all `CacheService` method calls and:
- Records metrics for every operation
- Adds distributed tracing spans (if OpenTelemetry is available)
- Logs operations with structured logging
- Handles errors gracefully

**No code changes required** - simply include this module and all cache operations are automatically instrumented!

### 3. Structured Logging (CacheStructuredLogger)
- Adds correlation IDs to all log statements using SLF4J MDC
- Logs at appropriate levels:
  - `DEBUG`: Cache hits, misses, puts, removes
  - `INFO`: Cache clear operations
  - `WARN`: Cache errors
- Includes contextual information: cache name, namespace, key, operation, duration

### 4. OpenTelemetry Tracing (CacheTracingService)
- Creates spans for all cache operations
- Adds attributes: cache.name, cache.namespace, cache.operation, cache.key, cache.hit
- Records exceptions and sets error status
- Propagates trace context automatically

### 5. Health Indicator (CacheHealthIndicator)
- Checks Redis connectivity (if configured)
- Reports cache statistics for all cache instances
- Exposes via Spring Boot Actuator `/actuator/health` endpoint

## Configuration

### Enable/Disable Observability

```yaml
immortals:
  cache:
    observability:
      enabled: true  # Default: true
      metrics:
        enabled: true  # Default: true
      tracing:
        enabled: true  # Default: true (requires OpenTelemetry)
```

### Health Indicator

```yaml
management:
  health:
    cache:
      enabled: true  # Default: true
```

## Usage

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>cache-observability</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 2. Metrics

Metrics are automatically exposed via Micrometer and can be scraped by Prometheus:

```
# Counters
cache_hits_total{cache="RedisCacheService",namespace="users"} 1234
cache_misses_total{cache="RedisCacheService",namespace="users"} 56
cache_evictions_total{cache="RedisCacheService",namespace="users"} 12

# Timers
cache_get_seconds_sum{cache="RedisCacheService",namespace="users"} 1.234
cache_get_seconds_count{cache="RedisCacheService",namespace="users"} 1290
cache_put_seconds_sum{cache="RedisCacheService",namespace="users"} 0.567

# Gauges
cache_size{cache="RedisCacheService",namespace="users"} 10000
cache_hit_rate{cache="RedisCacheService",namespace="users"} 0.956
cache_memory_usage_bytes{cache="RedisCacheService",namespace="users"} 1048576
```

### 3. Structured Logging

Logs include MDC context for correlation:

```json
{
  "timestamp": "2025-11-16T15:21:34.123Z",
  "level": "DEBUG",
  "message": "Cache hit - cache: RedisCacheService, namespace: users, key: user:123, duration: 2ms",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "cacheName": "RedisCacheService",
  "namespace": "users",
  "operation": "get",
  "key": "user:123",
  "durationMs": "2"
}
```

### 4. Distributed Tracing

When OpenTelemetry is configured, cache operations automatically create spans:

```
Trace: user-service-request
  └─ Span: cache.get
      ├─ cache.name: RedisCacheService
      ├─ cache.namespace: users
      ├─ cache.operation: cache.get
      ├─ cache.key: user:123
      ├─ cache.hit: true
      └─ duration: 2ms
```

### 5. Health Checks

Access health information via Actuator:

```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "cache": {
      "status": "UP",
      "details": {
        "redis": {
          "status": "UP",
          "connection": "active"
        },
        "caches": {
          "users": {
            "namespace": "users",
            "hitRate": "95.60%",
            "hitCount": 1234,
            "missCount": 56,
            "evictionCount": 12,
            "currentSize": 10000,
            "avgGetLatency": "2.34ms",
            "avgPutLatency": "3.45ms",
            "memoryUsage": "1.00 MB"
          }
        }
      }
    }
  }
}
```

## Architecture

The observability module uses AOP (Aspect-Oriented Programming) to automatically instrument all cache operations without requiring changes to existing code:

```
┌─────────────────────────────────────────┐
│     Application Code                    │
│  (Uses CacheService interface)          │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  CacheObservabilityAspect (AOP)         │
│  - Intercepts all CacheService calls    │
│  - Records metrics                      │
│  - Adds tracing spans                   │
│  - Logs operations                      │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  CacheService Implementation            │
│  (RedisCacheService, CaffeineCacheService, etc.) │
└─────────────────────────────────────────┘
```

## Benefits

1. **Zero Code Changes**: Existing cache implementations don't need modification
2. **Consistent Observability**: All cache operations are instrumented uniformly
3. **Performance**: Minimal overhead using efficient Micrometer metrics
4. **Flexibility**: Can be enabled/disabled via configuration
5. **Production-Ready**: Includes correlation IDs, error handling, and health checks

## Requirements

- Spring Boot 2.7+ or 3.x
- Micrometer (included with Spring Boot Actuator)
- OpenTelemetry (optional, for distributed tracing)
- SLF4J (for logging)

## See Also

- [Micrometer Documentation](https://micrometer.io/docs)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
