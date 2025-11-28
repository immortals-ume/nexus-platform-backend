# Cache Starter

A production-ready Spring Boot starter providing enterprise-grade caching with multiple providers, resilience patterns, and comprehensive observability.

## 🚀 Features

### Multi-Provider Support
- **Caffeine**: High-performance in-memory cache (L1)
- **Redis**: Distributed cache with clustering and sentinel support (L2)
- **Multi-Level**: Two-tier caching (Caffeine + Redis) with automatic synchronization

### Resilience Patterns
- **Circuit Breaker**: Prevents cascading failures with automatic fallback
- **Stampede Protection**: Distributed locking to prevent thundering herd
- **Retry Logic**: Exponential backoff for transient failures
- **Timeout Handling**: Configurable operation timeouts

### Advanced Features
- **Namespace Isolation**: Multiple cache instances with independent configuration
- **Compression**: Automatic GZIP compression for large values
- **Encryption**: AES-GCM encryption for sensitive data
- **Serialization**: JSON (Jackson) or Java serialization
- **Annotations**: Declarative caching with `@Cacheable`, `@CachePut`, `@CacheEvict`

### Observability
- **Metrics**: Hit/miss rates, latency, throughput (Micrometer)
- **Health Checks**: Spring Boot Actuator integration
- **Distributed Tracing**: OpenTelemetry support
- **Structured Logging**: Correlation IDs and contextual information

## 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>cache-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## ⚙️ Configuration

### Caffeine (In-Memory)

```yaml
immortals:
  cache:
    type: caffeine
    default-ttl: 1h
    caffeine:
      maximum-size: 10000
      ttl: 1h
      record-stats: true
```

### Redis (Distributed)

```yaml
immortals:
  cache:
    type: redis
    default-ttl: 1h
    redis-properties:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      command-timeout: 5s
      use-ssl: false
      auto-reconnect: true
      
      pool-max-total: 8
      pool-max-idle: 8
      pool-min-idle: 2
      
      pipelining:
        enabled: true
        batch-size: 100
      
      resilience:
        circuit-breaker:
          enabled: true
          failure-rate-threshold: 50
          wait-duration-in-open-state: 60s
        
        stampede-protection:
          enabled: true
          lock-timeout: 5s
```

### Multi-Level Cache

```yaml
immortals:
  cache:
    type: multi-level
    default-ttl: 1h
    
    caffeine:
      maximum-size: 10000
      ttl: 5m
    
    redis-properties:
      host: localhost
      port: 6379
      ttl: 1h
    
    multi-level:
      enabled: true
      eviction-enabled: true
      fallback-enabled: true
```

### Redis Cluster

```yaml
immortals:
  cache:
    redis-properties:
      cluster:
        nodes:
          - redis-node1:6379
          - redis-node2:6379
          - redis-node3:6379
```

### Redis Sentinel

```yaml
immortals:
  cache:
    redis-properties:
      sentinel:
        master: mymaster
        nodes:
          - sentinel1:26379
          - sentinel2:26379
          - sentinel3:26379
```

### Features Configuration

```yaml
immortals:
  cache:
    features:
      compression:
        enabled: true
        threshold: 1024
        algorithm: gzip
      
      encryption:
        enabled: true
        algorithm: AES-GCM
        key: ${CACHE_ENCRYPTION_KEY}
      
      serialization:
        strategy: jackson
    
    resilience:
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
      
      stampede-protection:
        enabled: true
        lock-timeout: 5s
    
    observability:
      metrics:
        enabled: true
      tracing:
        enabled: true
      logging:
        enabled: true
```

## 💻 Usage

### Programmatic API

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UnifiedCacheManager cacheManager;
    
    public User getUser(String userId) {
        CacheService<String, User> cache = cacheManager.getCache("users");
        
        return cache.get(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId);
                cache.put(userId, user, Duration.ofHours(1));
                return user;
            });
    }
    
    public void updateUser(User user) {
        userRepository.save(user);
        
        CacheService<String, User> cache = cacheManager.getCache("users");
        cache.put(user.getId(), user);
    }
    
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
        
        CacheService<String, User> cache = cacheManager.getCache("users");
        cache.remove(userId);
    }
}
```

### Declarative Caching with Annotations

```java
@Service
public class ProductService {
    
    @Cacheable(
        namespace = "products",
        key = "#productId",
        ttl = 3600,
        stampedeProtection = true
    )
    public Product getProduct(String productId) {
        return productRepository.findById(productId);
    }
    
    @CachePut(
        namespace = "products",
        key = "#product.id",
        ttl = 3600
    )
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
    
    @CacheEvict(
        namespace = "products",
        key = "#productId"
    )
    public void deleteProduct(String productId) {
        productRepository.deleteById(productId);
    }
    
    @CacheEvict(
        namespace = "products",
        allEntries = true
    )
    public void clearAllProducts() {
        productRepository.deleteAll();
    }
}
```

### Conditional Caching

```java
@Cacheable(
    namespace = "users",
    key = "#userId",
    condition = "#userId != null",
    unless = "#result == null"
)
public User findUser(String userId) {
    return userRepository.findById(userId);
}
```

### Batch Operations

```java
CacheService<String, Product> cache = cacheManager.getCache("products");

Map<String, Product> products = Map.of(
    "prod1", product1,
    "prod2", product2,
    "prod3", product3
);
// cache.putAll(products);

List<String> keys = List.of("prod1", "prod2", "prod3");
Map<String, Product> cached = cache.getAll(keys);
```

### Atomic Operations

```java
CacheService<String, Long> cache = cacheManager.getCache("counters");

Long views = cache.increment("product:123:views", 1);

boolean added = cache.putIfAbsent("lock:order:456", "processing");
if (added) {
}
```

### Namespace-Specific Configuration

```java
CacheConfiguration config = new CacheConfiguration();
config.setTtl(Duration.ofMinutes(30));
config.setCompressionEnabled(true);
config.setEncryptionEnabled(true);

CacheService<String, SensitiveData> cache = 
    cacheManager.getCache("sensitive-data", config);
```

## 📊 Monitoring

### Metrics

Available metrics (via Micrometer):

- `cache.hits`: Number of cache hits
- `cache.misses`: Number of cache misses
- `cache.puts`: Number of put operations
- `cache.evictions`: Number of evictions
- `cache.get.duration`: Get operation latency
- `cache.put.duration`: Put operation latency
- `cache.size`: Current cache size
- `cache.hit.rate`: Cache hit rate (0.0 to 1.0)

All metrics are tagged with:
- `namespace`: Cache namespace
- `provider`: Cache provider (caffeine, redis, multi-level)

### Health Checks

Access health information via Spring Boot Actuator:

```bash
curl http://localhost:8080/actuator/health/cache
```

Response:
```json
{
  "status": "UP",
  "details": {
    "redis": {
      "status": "UP",
      "connection": "active"
    },
    "caches": {
      "users": {
        "hitRate": "85.50%",
        "hitCount": 1710,
        "missCount": 290,
        "currentSize": 450
      }
    }
  }
}
```

### Statistics

```java
CacheService<String, User> cache = cacheManager.getCache("users");
CacheStatistics stats = cache.getStatistics();

System.out.println("Hit Rate: " + stats.getHitRate());
System.out.println("Avg Get Latency: " + stats.getAvgGetLatency() + "ms");
System.out.println("Current Size: " + stats.getCurrentSize());
```

## 🏗️ Architecture

### Modules

- **cache-core**: Core interfaces and abstractions
- **cache-providers**: Caffeine, Redis, and multi-level implementations
- **cache-features**: Compression, encryption, annotations, serialization
- **cache-observability**: Metrics, health checks, tracing, logging

### Design Patterns

- **Strategy Pattern**: Pluggable cache providers
- **Decorator Pattern**: Layered features (compression, encryption, metrics)
- **Factory Pattern**: Cache service creation
- **Template Method**: Event handling and retry logic

## 🔒 Security

### Encryption

Generate an encryption key:

```bash
openssl rand -base64 32
```

Configure in application:

```yaml
immortals:
  cache:
    features:
      encryption:
        enabled: true
        key: ${CACHE_ENCRYPTION_KEY}
```

### SSL/TLS for Redis

```yaml
immortals:
  cache:
    redis-properties:
      use-ssl: true
      ssl:
        trust-store: /path/to/truststore.jks
        trust-store-password: ${TRUSTSTORE_PASSWORD}
        key-store: /path/to/keystore.jks
        key-store-password: ${KEYSTORE_PASSWORD}
```

### ACL Authentication

```yaml
immortals:
  cache:
    redis-properties:
      acl:
        enabled: true
        username: cache-user
      password: ${REDIS_PASSWORD}
```

## 🚨 Error Handling

The cache starter provides graceful degradation:

- **Circuit Breaker Open**: Falls back to L1 cache or returns empty
- **Redis Connection Failure**: Continues with L1 cache only
- **Serialization Errors**: Logs error and returns empty
- **Timeout**: Cancels operation and returns empty

All errors are logged with correlation IDs for troubleshooting.

## 🧪 Testing

### Unit Testing

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UnifiedCacheManager cacheManager;
    
    @Test
    void testCaching() {
        CacheService<String, User> cache = cacheManager.getCache("test");
        
        User user = new User("123", "John");
        cache.put(user.getId(), user);
        
        Optional<User> cached = cache.get("123");
        assertThat(cached).isPresent();
        assertThat(cached.get().getName()).isEqualTo("John");
    }
}
```

### Integration Testing

```java
@SpringBootTest
@Testcontainers
class CacheIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("immortals.cache.redis-properties.host", redis::getHost);
        registry.add("immortals.cache.redis-properties.port", redis::getFirstMappedPort);
    }
    
    @Test
    void testRedisCache() {
    }
}
```

## 📚 Best Practices

1. **Use Namespaces**: Isolate different data types in separate namespaces
2. **Set Appropriate TTLs**: Balance freshness vs. cache hit rate
3. **Enable Compression**: For values > 1KB
4. **Enable Encryption**: For sensitive data (PII, tokens)
5. **Monitor Metrics**: Track hit rates and latencies
6. **Use Multi-Level**: For high-traffic applications
7. **Enable Stampede Protection**: For expensive computations
8. **Configure Circuit Breaker**: Prevent cascading failures

## 🔧 Troubleshooting

### High Miss Rate

- Check TTL configuration
- Verify cache key generation
- Monitor eviction rate

### High Latency

- Enable pipelining for batch operations
- Check network latency to Redis
- Consider using multi-level cache

### Memory Issues

- Reduce Caffeine maximum size
- Enable compression
- Set appropriate TTLs

### Connection Failures

- Verify Redis connectivity
- Check firewall rules
- Enable auto-reconnect
- Configure circuit breaker

## 📖 Requirements Satisfied

This starter satisfies the following platform requirements:

- **5.1**: Pluggable cache providers (Caffeine, Redis, Multi-level)
- **5.2**: Circuit breaker pattern for fault tolerance
- **5.3**: Distributed caching with Redis
- **5.4**: Stampede protection with distributed locks
- **5.5**: Timeout handling for cache operations
- **6.1**: Compression and encryption support
- **6.2**: Configurable serialization strategies
- **7.1**: Comprehensive metrics collection
- **7.2**: Health check integration
- **8.1**: Namespace isolation
- **8.2**: Per-namespace configuration
- **10.1**: Declarative caching with annotations
- **10.2**: Programmatic cache API

## 📄 License

Copyright © 2024 Immortals Platform

## 🤝 Contributing

For questions or issues, contact the platform team.
