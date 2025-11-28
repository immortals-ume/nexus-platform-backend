# Cache Annotation Support

Declarative caching using Spring-style annotations for the cache service.

## Annotations

### @Cacheable

Read-through caching - checks cache before method execution.

```java

@Cacheable(namespace = "users", key = "#userId")
public User findUser(String userId) {
    return userRepository.findById(userId);
}
```

**Attributes:**

- `namespace` (required): Cache namespace
- `key`: SpEL expression for key generation
- `condition`: SpEL condition to enable caching
- `unless`: SpEL condition to skip caching based on result
- `ttl`: Time-to-live in seconds
- `compress`: Enable compression
- `encrypt`: Enable encryption
- `stampedeProtection`: Enable stampede protection

### @CachePut

Write-through caching - always executes and updates cache.

```java

@CachePut(namespace = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

**Attributes:**

- `namespace` (required): Cache namespace
- `key`: SpEL expression for key generation
- `condition`: SpEL condition to enable caching
- `unless`: SpEL condition to skip caching based on result
- `ttl`: Time-to-live in seconds
- `compress`: Enable compression
- `encrypt`: Enable encryption

### @CacheEvict

Cache invalidation - removes entries from cache.

```java

@CacheEvict(namespace = "users", key = "#userId")
public void deleteUser(String userId) {
    userRepository.deleteById(userId);
}

@CacheEvict(namespace = "users", allEntries = true)
public void deleteAllUsers() {
    userRepository.deleteAll();
}
```

**Attributes:**

- `namespace` (required): Cache namespace
- `key`: SpEL expression for key generation
- `condition`: SpEL condition to enable eviction
- `allEntries`: Clear entire namespace
- `beforeInvocation`: Evict before method execution

## SpEL Expression Support

### Key Generation

Reference method parameters:

```java
// By name
@Cacheable(namespace = "users", key = "#userId")
public User findUser(String userId) { ...}

// By position
@Cacheable(namespace = "users", key = "#p0")
public User findUser(String userId) { ...}

// Object properties
@Cacheable(namespace = "users", key = "#user.id")
public User saveUser(User user) { ...}

// Concatenation
@Cacheable(namespace = "users", key = "'user:' + #userId")
public User findUser(String userId) { ...}
```

### Conditional Caching

```java
// Cache only if parameter is not null
@Cacheable(
        namespace = "users",
        key = "#userId",
        condition = "#userId != null"
)
public User findUser(String userId) { ...}

// Don't cache null results
@Cacheable(
        namespace = "users",
        key = "#userId",
        unless = "#result == null"
)
public User findUser(String userId) { ...}

// Complex conditions
@Cacheable(
        namespace = "users",
        key = "#userId",
        condition = "#userId != null && #userId.length() > 0",
        unless = "#result == null || #result.isDeleted()"
)
public User findUser(String userId) { ...}
```

## Usage Examples

### Basic Caching

```java

@Service
public class UserService {

    @Cacheable(namespace = "users", key = "#id")
    public User getUser(String id) {
        // This method is only called on cache miss
        return userRepository.findById(id)
                .orElse(null);
    }

    @CachePut(namespace = "users", key = "#user.id")
    public User updateUser(User user) {
        // Always executes and updates cache
        return userRepository.save(user);
    }

    @CacheEvict(namespace = "users", key = "#id")
    public void deleteUser(String id) {
        // Removes from cache before deletion
        userRepository.deleteById(id);
    }
}
```

### With TTL and Features

```java

@Service
public class SessionService {

    @Cacheable(
            namespace = "sessions",
            key = "#sessionId",
            ttl = 1800,  // 30 minutes
            encrypt = true,  // Encrypt sensitive session data
            stampedeProtection = true  // Prevent concurrent loads
    )
    public Session getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @CachePut(
            namespace = "sessions",
            key = "#session.id",
            ttl = 1800,
            encrypt = true
    )
    public Session updateSession(Session session) {
        return sessionRepository.save(session);
    }
}
```

### Conditional Caching

```java

@Service
public class ProductService {

    // Only cache if product is active
    @Cacheable(
            namespace = "products",
            key = "#id",
            condition = "#includeInactive == false"
    )
    public Product getProduct(String id, boolean includeInactive) {
        return productRepository.findById(id);
    }

    // Don't cache empty results
    @Cacheable(
            namespace = "products",
            key = "#category",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}
```

### Bulk Operations

```java

@Service
public class CacheManagementService {

    // Clear entire namespace
    @CacheEvict(namespace = "users", allEntries = true)
    public void clearAllUsers() {
        log.info("Cleared all users from cache");
    }

    // Evict before method execution (safer for deletes)
    @CacheEvict(
            namespace = "users",
            key = "#userId",
            beforeInvocation = true
    )
    public void deleteUser(String userId) {
        // Cache is cleared even if this throws an exception
        userRepository.deleteById(userId);
    }
}
```

## Configuration

Enable/disable annotation support:

```yaml
immortals:
  cache:
    annotations:
      enabled: true  # Default: true
```

## How It Works

1. **Auto-Configuration**: `CacheAnnotationAutoConfiguration` automatically registers the `CacheAspect` bean
2. **AOP Interception**: `CacheAspect` intercepts methods annotated with cache annotations
3. **Key Generation**: `KeyGenerator` evaluates SpEL expressions to generate cache keys
4. **Condition Evaluation**: `ExpressionEvaluator` evaluates condition and unless expressions
5. **Cache Operations**: Aspect delegates to appropriate `CacheService` based on namespace

## Benefits

- ✅ **Declarative**: No boilerplate cache code in business logic
- ✅ **Flexible**: SpEL expressions for dynamic keys and conditions
- ✅ **Feature-Rich**: Supports TTL, compression, encryption, stampede protection
- ✅ **Spring-Compatible**: Similar to Spring Cache annotations
- ✅ **Type-Safe**: Compile-time checking of annotation attributes

## Comparison with Spring Cache

| Feature             | Spring Cache | Immortals Cache |
|---------------------|--------------|-----------------|
| @Cacheable          | ✅            | ✅               |
| @CachePut           | ✅            | ✅               |
| @CacheEvict         | ✅            | ✅               |
| SpEL Support        | ✅            | ✅               |
| TTL per annotation  | ❌            | ✅               |
| Compression         | ❌            | ✅               |
| Encryption          | ❌            | ✅               |
| Stampede Protection | ❌            | ✅               |
| Namespace isolation | ✅            | ✅               |

## Requirements

- Spring Boot 2.7+ or 3.x
- Spring AOP (included with spring-boot-starter-aop)
- AspectJ runtime
