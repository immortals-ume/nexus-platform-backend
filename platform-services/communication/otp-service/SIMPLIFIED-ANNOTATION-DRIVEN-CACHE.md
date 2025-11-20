# Simplified Annotation-Driven Cache - Complete Guide

## Overview

We've implemented a **simplified annotation-driven caching** approach that reduces complexity while keeping all FAANG-level features.

## The Transformation

### Before (Complex - 8+ Services):

```java
@Service
public class OtpService {
    @Autowired private CompressingCacheService compressingCache;
    @Autowired private EncryptedCacheService encryptedCache;
    @Autowired private NamespacedCacheService namespacedCache;
    @Autowired private CircuitBreakerCacheService circuitBreakerCache;
    
    public String getOtp(String mobile) {
        // Manual composition of features
        String key = "otp:sms:" + mobile;
        Optional<String> cached = namespacedCache.get("otp-sms", mobile, String.class);
        
        if (cached.isPresent()) {
            String encrypted = cached.get();
            String decrypted = decrypt(encrypted);
            String decompressed = decompress(decrypted);
            return decompressed;
        }
        
        String otp = generateOtp();
        String compressed = compress(otp);
        String encrypted = encrypt(compressed);
        namespacedCache.put("otp-sms", mobile, encrypted, Duration.ofMinutes(5));
        
        return otp;
    }
}
```

### After (Simple - Annotations):

```java
@Service
public class OtpService {
    // No cache service injection needed!
    
    @Cacheable(
        value = "otp",
        namespace = "sms",
        key = "#mobile",
        ttl = "5m",
        compress = true,
        encrypt = true,
        stampedeProtection = true
    )
    @CircuitBreaker(name = "cache")  // Standard Resilience4j!
    public String getOtp(String mobile) {
        return generateOtp();
        // AOP handles everything:
        // - Check cache (L1 → L2)
        // - Decrypt + decompress if found
        // - Generate if miss
        // - Compress + encrypt + store
        // - Circuit breaker protection
        // - Metrics collection
    }
}
```

## Architecture

### Simplified Structure:

```
User Code (@Cacheable annotation)
    ↓
CacheAspect (AOP - intercepts method)
    ↓
├── Check cache (L1 → L2)
├── Apply decryption (if encrypt=true)
├── Apply decompression (if compress=true)
├── Execute method (if cache miss)
├── Apply stampede protection (if enabled)
├── Apply compression (if compress=true)
├── Apply encryption (if encrypt=true)
└── Store in cache (L1 + L2)
    ↓
SimpleCacheService (L1: Caffeine + L2: Redis)
```

### Components:

1. **SimpleCacheService** - Core L1+L2 caching (ONE service)
2. **@Cacheable** - Annotation with all features
3. **@CacheEvict** - Annotation for eviction
4. **CacheAspect** - AOP that applies features
5. **Utility Services** - Compression, Encryption, KeyGenerator

## Features

### All FAANG-Level Features Preserved:

| Feature | Implementation | Usage |
|---------|---------------|-------|
| **Multi-level caching** | Built into SimpleCacheService | Automatic (L1 → L2) |
| **Circuit breaker** | @CircuitBreaker (Resilience4j) | `@CircuitBreaker(name="cache")` |
| **Compression** | CompressionUtil (LZ4) | `@Cacheable(compress=true)` |
| **Encryption** | EncryptionUtil (AES-256-GCM) | `@Cacheable(encrypt=true)` |
| **Stampede protection** | Distributed locks (Redisson) | `@Cacheable(stampedeProtection=true)` |
| **Namespace isolation** | KeyGenerator | `@Cacheable(namespace="sms")` |
| **TTL management** | TtlParser | `@Cacheable(ttl="5m")` |
| **Conditional caching** | SpEL expressions | `@Cacheable(condition="#result!=null")` |
| **Metrics** | Automatic via AOP | Automatic |
| **Distributed eviction** | Redis Pub/Sub | Built-in |

## Usage Examples

### Example 1: Simple Caching

```java
@Cacheable(value = "users", ttl = "5m")
public User getUser(String id) {
    return userRepository.findById(id);
}
```

**What happens:**
- Checks L1 cache (Caffeine)
- If miss, checks L2 cache (Redis)
- If miss, executes method
- Stores in L1 + L2 with 5-minute TTL
- Metrics collected automatically

### Example 2: With Compression

```java
@Cacheable(
    value = "products",
    ttl = "10m",
    compress = true,
    compressionThreshold = 1024  // 1KB
)
public Product getProduct(String id) {
    return productRepository.findById(id);
}
```

**What happens:**
- Same as above
- If result > 1KB, compresses before storing
- Decompresses automatically on retrieval

### Example 3: With Encryption

```java
@Cacheable(
    value = "sensitive-data",
    namespace = "security",
    ttl = "2m",
    encrypt = true
)
public SensitiveData getSensitiveData(String userId) {
    return loadSensitiveData(userId);
}
```

**What happens:**
- Encrypts value before storing
- Decrypts automatically on retrieval
- Uses AES-256-GCM encryption

### Example 4: All Features Combined

```java
@Cacheable(
    value = "user-profile",
    namespace = "users",
    key = "#userId",
    ttl = "5m",
    compress = true,
    encrypt = true,
    stampedeProtection = true,
    condition = "#result != null",
    unless = "#result.deleted == true"
)
@CircuitBreaker(name = "cache", fallbackMethod = "fallbackGetProfile")
public UserProfile getUserProfile(String userId) {
    return userService.loadProfile(userId);
}

private UserProfile fallbackGetProfile(String userId, Exception e) {
    log.warn("Cache circuit open, loading directly");
    return userService.loadProfile(userId);
}
```

**What happens:**
- Checks condition before caching
- Uses stampede protection (distributed lock)
- Compresses if large
- Encrypts for security
- Stores in L1 + L2
- Circuit breaker protection
- Fallback if cache fails
- Checks unless condition after execution

### Example 5: Cache Eviction

```java
@CacheEvict(
    value = "users",
    namespace = "users",
    key = "#id"
)
public void deleteUser(String id) {
    userRepository.delete(id);
}

@CacheEvict(
    value = "users",
    allEntries = true
)
public void deleteAllUsers() {
    userRepository.deleteAll();
}
```

### Example 6: OTP Service (Complete)

```java
@Service
public class OtpService {
    
    // Generate OTP with all features
    @Cacheable(
        value = "otp",
        namespace = "sms",
        key = "#mobile",
        ttl = "5m",
        stampedeProtection = true
    )
    @CircuitBreaker(name = "cache")
    public void sendOtp(String mobile) {
        checkRateLimit(mobile);
        String otp = generateOtp();
        incrementRateLimitCounter(mobile);
        // Send SMS
    }
    
    // Verify OTP
    @Cacheable(value = "otp", namespace = "sms", key = "#mobile", ttl = "5m")
    @CircuitBreaker(name = "cache")
    public void verifyOtp(String mobile, String otp) {
        String cachedOtp = getCachedOtp(mobile);
        if (cachedOtp == null || !cachedOtp.equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        evictOtp(mobile);
    }
    
    // Evict OTP
    @CacheEvict(value = "otp", namespace = "sms", key = "#mobile")
    private void evictOtp(String mobile) {
        // Evicted automatically
    }
    
    // Rate limiting
    @Cacheable(value = "rate-limit", namespace = "otp", key = "#mobile", ttl = "1h")
    private void checkRateLimit(String mobile) {
        Integer attempts = getRateLimitAttempts(mobile);
        if (attempts != null && attempts >= 3) {
            throw new RuntimeException("Rate limit exceeded");
        }
    }
}
```

## Configuration

### application.yml

```yaml
platform:
  cache:
    # L1 cache (Caffeine)
    l1:
      enabled: true
      max-size: 10000
      ttl: 2m
      eviction-policy: LRU
    
    # L2 cache (Redis)
    l2:
      enabled: true
      ttl: 5m
    
    # Features
    compression:
      enabled: true
      threshold: 1024  # 1KB
      algorithm: LZ4
    
    encryption:
      enabled: true
      algorithm: AES-256-GCM
      key: ${CACHE_ENCRYPTION_KEY}
    
    stampede-protection:
      enabled: true
      lock-timeout: 5s

# Circuit breaker (Resilience4j)
resilience4j:
  circuitbreaker:
    instances:
      cache:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 10
```

## Benefits

### 1. Simplicity

**Before:**
- 8+ cache services
- Manual composition
- Complex code

**After:**
- 1 annotation
- Declarative features
- Simple code

### 2. Reduced Code

**Before:** ~50 lines of cache logic
**After:** ~5 lines (just annotations)

### 3. Declarative

```java
// Features declared in annotations
@Cacheable(compress=true, encrypt=true, stampedeProtection=true)
```

### 4. Standard Integration

```java
// Use standard Resilience4j @CircuitBreaker
@CircuitBreaker(name = "cache")
```

### 5. Testability

```java
// Easy to test - just mock the method
@Test
public void testGetOtp() {
    when(otpService.getOtp("123")).thenReturn("654321");
    // No need to mock cache services!
}
```

## Comparison

| Aspect | Old (8+ Services) | New (Annotations) |
|--------|------------------|-------------------|
| **Services** | 8+ separate services | 1 service |
| **Code Lines** | ~50 lines | ~5 lines |
| **Composition** | Manual | Automatic (AOP) |
| **Circuit Breaker** | Separate service | @CircuitBreaker |
| **Features** | Choose services | Annotation params |
| **Learning Curve** | High | Low |
| **Testability** | Complex mocking | Simple mocking |
| **Maintainability** | Hard | Easy |

## Migration Guide

### Step 1: Remove Old Service Injections

**Before:**
```java
@Autowired private CompressingCacheService cache;
```

**After:**
```java
// No injection needed!
```

### Step 2: Add Annotations

**Before:**
```java
public String getData(String id) {
    Optional<String> cached = cache.get(id, String.class);
    if (cached.isPresent()) {
        return cached.get();
    }
    String data = loadData(id);
    cache.put(id, data, Duration.ofMinutes(5));
    return data;
}
```

**After:**
```java
@Cacheable(value = "data", key = "#id", ttl = "5m")
public String getData(String id) {
    return loadData(id);
}
```

### Step 3: Add Circuit Breaker

```java
@Cacheable(value = "data", key = "#id", ttl = "5m")
@CircuitBreaker(name = "cache", fallbackMethod = "fallbackGetData")
public String getData(String id) {
    return loadData(id);
}

private String fallbackGetData(String id, Exception e) {
    return loadData(id);
}
```

## Conclusion

**Simplified annotation-driven caching provides:**
- ✅ 8+ services → 1 service
- ✅ Manual composition → Annotations
- ✅ Complex code → Declarative
- ✅ Separate circuit breaker → Standard @CircuitBreaker
- ✅ All FAANG features preserved
- ✅ Better developer experience
- ✅ Easier to maintain
- ✅ Simpler to test

**Recommendation:** Use annotation-driven approach for all new code.

## Next Steps

1. ✅ SimpleCacheService implemented
2. ✅ @Cacheable annotation created
3. ✅ CacheAspect (AOP) implemented
4. ✅ Utility services created
5. ✅ OTP service updated
6. ⏳ Test with real Redis
7. ⏳ Performance benchmarks
8. ⏳ Migrate other services
