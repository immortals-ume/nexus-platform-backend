# Cache Service Options in cache-starter

## Available Cache Service Implementations

The cache-starter provides multiple cache service implementations. You can choose based on your requirements:

### 1. **NamespacedCacheService** (Recommended for Multi-Tenant/Isolated Caching)
**Use When:** You need namespace isolation (e.g., different OTP types, multi-tenant data)

```java
@Autowired
private NamespacedCacheService cacheService;

// Usage with namespace
cacheService.put("otp-sms", "key", value, Duration.ofMinutes(5));
cacheService.get("otp-sms", "key", String.class);
```

**Benefits:**
- Automatic key prefixing (prevents collisions)
- Namespace-specific metrics
- Namespace-based cache clearing
- Perfect for multi-tenant or categorized data

---

### 2. **MultiLevelCacheService** (L1 + L2 Caching)
**Use When:** You want automatic L1 (Caffeine) + L2 (Redis) caching

```java
@Autowired
private MultiLevelCacheService cacheService;

// Automatically uses L1 (local) and L2 (Redis)
cacheService.put("key", value, Duration.ofMinutes(5));
Optional<String> result = cacheService.get("key", String.class);
```

**Benefits:**
- L1 cache (Caffeine) for ultra-fast local access (<1ms)
- L2 cache (Redis) for distributed caching
- Automatic cache invalidation across instances
- Read-through: L1 → L2 → Source
- Write-through: Both L1 and L2

---

### 3. **CacheService** (Generic Interface)
**Use When:** You want a simple, generic cache interface

```java
@Autowired
private CacheService cacheService;

// Simple caching
cacheService.put("key", value);
Optional<String> result = cacheService.get("key", String.class);
```

**Benefits:**
- Simple API
- Works with any underlying implementation
- Good for basic caching needs

---

### 4. **RedisCacheService** (Redis Only)
**Use When:** You only need distributed Redis caching (no local cache)

```java
@Autowired
private RedisCacheService redisCacheService;

// Direct Redis operations
redisCacheService.put("key", value, Duration.ofMinutes(5));
Optional<String> result = redisCacheService.get("key", String.class);
```

**Benefits:**
- Direct Redis access
- Distributed caching across all instances
- No local cache overhead
- Good for shared data that changes frequently

---

### 5. **CaffeineMultiLevelCacheService** (Caffeine + Redis)
**Use When:** You want explicit control over L1/L2 caching

```java
@Autowired
private CaffeineMultiLevelCacheService cacheService;

// Explicit multi-level caching
cacheService.put("key", value, Duration.ofMinutes(5));
```

**Benefits:**
- Same as MultiLevelCacheService but with Caffeine-specific features
- Access to Caffeine statistics
- Fine-grained control

---

### 6. **CompressingCacheService** (With Compression)
**Use When:** You're caching large objects and want automatic compression

```java
@Autowired
private CompressingCacheService cacheService;

// Automatically compresses large values
cacheService.put("key", largeObject, Duration.ofMinutes(5));
```

**Benefits:**
- Automatic LZ4 compression for values > threshold
- Reduces memory usage
- Reduces network bandwidth
- Transparent decompression

---

### 7. **ConditionalCacheService** (With Predicates)
**Use When:** You want to cache only if certain conditions are met

```java
@Autowired
private ConditionalCacheService cacheService;

// Only caches if predicate passes
CachePredicate nonNullPredicate = CachePredicates.nonNull();
cacheService.putIfPredicate("key", value, nonNullPredicate, Duration.ofMinutes(5));
```

**Benefits:**
- Conditional caching based on value properties
- Prevents caching invalid/temporary data
- Built-in predicates (non-null, size limits)

---

### 8. **HashCacheService** (Redis Hash Operations)
**Use When:** You need Redis hash operations (field-level caching)

```java
@Autowired
private HashCacheService hashCacheService;

// Hash operations
hashCacheService.putField("user:123", "name", "John");
hashCacheService.putField("user:123", "email", "john@example.com");
Optional<String> name = hashCacheService.getField("user:123", "name", String.class);
```

**Benefits:**
- Field-level operations
- Efficient for structured data
- Atomic field updates
- Good for user profiles, configurations

---

## Comparison Table

| Service | L1 (Local) | L2 (Redis) | Namespace | Compression | Predicates | Use Case |
|---------|------------|------------|-----------|-------------|------------|----------|
| NamespacedCacheService | ✅ | ✅ | ✅ | ❌ | ❌ | Multi-tenant, isolated data |
| MultiLevelCacheService | ✅ | ✅ | ❌ | ❌ | ❌ | High-performance caching |
| CacheService | ✅ | ✅ | ❌ | ❌ | ❌ | Simple caching |
| RedisCacheService | ❌ | ✅ | ❌ | ❌ | ❌ | Distributed only |
| CompressingCacheService | ✅ | ✅ | ❌ | ✅ | ❌ | Large objects |
| ConditionalCacheService | ✅ | ✅ | ❌ | ❌ | ✅ | Conditional caching |
| HashCacheService | ❌ | ✅ | ❌ | ❌ | ❌ | Structured data |

---

## OTP Service - Different Implementation Examples

### Option 1: Using MultiLevelCacheService (L1 + L2)

```java
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private final MultiLevelCacheService cacheService; // L1 + L2 automatic
    
    @Override
    public void sendOtp(String mobile) {
        // Automatically uses L1 (Caffeine) and L2 (Redis)
        String otp = generateOtp();
        cacheService.put("otp:" + mobile, otp, Duration.ofMinutes(5));
        
        // First request: L1 miss → L2 miss → Generate
        // Second request: L1 hit (ultra-fast <1ms)
        // Another instance: L1 miss → L2 hit (fast ~2ms)
    }
    
    @Override
    public void verifyOtp(String mobile, String otp) {
        Optional<String> cached = cacheService.get("otp:" + mobile, String.class);
        // Checks L1 first, then L2 if L1 miss
        
        if (cached.isPresent() && cached.get().equals(otp)) {
            cacheService.remove("otp:" + mobile); // Removes from both L1 and L2
        }
    }
}
```

### Option 2: Using RedisCacheService (Redis Only)

```java
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private final RedisCacheService redisCacheService; // Redis only
    
    @Override
    public void sendOtp(String mobile) {
        String otp = generateOtp();
        redisCacheService.put("otp:" + mobile, otp, Duration.ofMinutes(5));
        // All instances share same Redis cache
        // No local cache, always goes to Redis
    }
}
```

### Option 3: Using HashCacheService (Redis Hash)

```java
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private final HashCacheService hashCacheService;
    
    @Override
    public void sendOtp(String mobile) {
        String otp = generateOtp();
        
        // Store OTP with metadata in Redis hash
        hashCacheService.putField("otp:" + mobile, "code", otp);
        hashCacheService.putField("otp:" + mobile, "createdAt", Instant.now().toString());
        hashCacheService.putField("otp:" + mobile, "attempts", "0");
        hashCacheService.expire("otp:" + mobile, Duration.ofMinutes(5));
    }
    
    @Override
    public void verifyOtp(String mobile, String otp) {
        Optional<String> cached = hashCacheService.getField("otp:" + mobile, "code", String.class);
        Optional<String> attempts = hashCacheService.getField("otp:" + mobile, "attempts", String.class);
        
        // Increment attempts
        int attemptCount = Integer.parseInt(attempts.orElse("0")) + 1;
        hashCacheService.putField("otp:" + mobile, "attempts", String.valueOf(attemptCount));
    }
}
```

### Option 4: Using NamespacedCacheService (Current Implementation)

```java
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private final NamespacedCacheService cacheService;
    
    @Override
    public void sendOtp(String mobile) {
        // Uses namespace for isolation
        // Actual Redis key: "otp:sms:+1234567890"
        cacheService.put("otp-sms", mobile, otp, Duration.ofMinutes(5));
        
        // Different namespace for rate limiting
        // Actual Redis key: "otp:rate:attempts:+1234567890"
        cacheService.put("otp-rate-limit", "attempts:" + mobile, 1, Duration.ofHours(1));
    }
}
```

---

## Recommendation for OTP Service

**Best Choice: MultiLevelCacheService** (L1 + L2)

**Why:**
1. **Performance**: L1 cache gives <1ms response for repeated verifications
2. **Scalability**: L2 cache (Redis) shares data across all instances
3. **Reliability**: If Redis fails, L1 cache still works (circuit breaker)
4. **Automatic**: No need to manage namespaces manually

**Updated Implementation:**

```java
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    
    private final MultiLevelCacheService cacheService; // Change to MultiLevelCacheService
    
    @Override
    public void sendOtp(String mobile) {
        String otpKey = "otp:" + mobile;
        String rateLimitKey = "rate:" + mobile;
        
        // Check rate limit
        Optional<Integer> attempts = cacheService.get(rateLimitKey, Integer.class);
        if (attempts.isPresent() && attempts.get() >= 3) {
            throw new RuntimeException("Rate limit exceeded");
        }
        
        // Generate OTP with stampede protection
        String otp = cacheService.getOrCompute(
            otpKey,
            String.class,
            this::generateOtp,
            Duration.ofMinutes(5)
        ).orElseThrow();
        
        // Increment rate limit
        cacheService.put(rateLimitKey, attempts.orElse(0) + 1, Duration.ofHours(1));
    }
    
    @Override
    public void verifyOtp(String mobile, String otp) {
        String otpKey = "otp:" + mobile;
        
        // L1 hit: <1ms
        // L1 miss, L2 hit: ~2ms
        // Both miss: generate new
        Optional<String> cached = cacheService.get(otpKey, String.class);
        
        if (cached.isEmpty() || !cached.get().equals(otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        // Remove from both L1 and L2
        cacheService.remove(otpKey);
    }
}
```

---

## How to Switch Between Implementations

Just change the autowired service:

```java
// Option 1: Multi-level (L1 + L2) - RECOMMENDED
@Autowired
private MultiLevelCacheService cacheService;

// Option 2: Namespaced (with isolation)
@Autowired
private NamespacedCacheService cacheService;

// Option 3: Redis only
@Autowired
private RedisCacheService cacheService;

// Option 4: Hash operations
@Autowired
private HashCacheService cacheService;

// Option 5: With compression
@Autowired
private CompressingCacheService cacheService;
```

All services are auto-configured by cache-starter. Just inject what you need!
