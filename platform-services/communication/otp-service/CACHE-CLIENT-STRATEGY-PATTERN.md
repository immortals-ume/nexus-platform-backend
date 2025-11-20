# CacheClient with Strategy Pattern - Complete Guide

## Overview

We've implemented a **CacheClient** using the **Strategy Pattern** that simplifies cache usage by hiding the complexity of multiple implementations.

## Problem Solved

### Before (Complex):
```java
// Too many choices!
@Autowired private MultiLevelCacheService cache1;
@Autowired private DistributedRedisCacheService cache2;
@Autowired private CompressingCacheService cache3;
@Autowired private EncryptedCacheService cache4;

// Which one to use? How to compose them?
```

### After (Simple):
```java
// One simple client!
@Autowired private CacheClient cache;

// Use it - strategy is configured via YAML
cache.put("key", value);
cache.get("key", String.class);
```

## Architecture

```
User Code
    ↓
CacheClient (Facade)
    ↓
CacheStrategy (Strategy Pattern)
    ├── MultiLevelStrategy (L1 + L2)
    ├── RedisOnlyStrategy (Redis only)
    ├── HashStrategy (Redis Hash)
    └── UnifiedStrategy (All features)
    
Each strategy can be decorated with:
    ├── CompressingDecorator (auto-compression)
    ├── EncryptedDecorator (auto-encryption)
    └── DistributedEvictionDecorator (cross-instance invalidation)
```

## Configuration

### application.yml

```yaml
cache:
  redis:
    # Strategy selection
    strategy:
      type: MULTI_LEVEL  # Options: MULTI_LEVEL, REDIS_ONLY, HASH, UNIFIED
    
    # Feature toggles (applied as decorators)
    compression:
      enabled: true
      thresholdBytes: 1024
      algorithm: GZIP
    
    encryption:
      enabled: false
      algorithm: AES-256-GCM
      keyBase64: <your-base64-key>
    
    eviction:
      enabled: true  # Distributed eviction
      channelName: cache:eviction
    
    # L1 cache settings (for MULTI_LEVEL strategy)
    l1Cache:
      enabled: true
      maxSize: 10000
      ttl: 60s
      evictionPolicy: LRU
```

## Strategy Types

### 1. MULTI_LEVEL (Recommended)
**Best for**: Most use cases

**Features:**
- L1 cache (Caffeine): Ultra-fast local cache (<1ms)
- L2 cache (Redis): Distributed cache (~2ms)
- Automatic L1 population from L2
- Cross-instance invalidation

**Use when:**
- You want best performance
- You have multiple instances
- You need distributed caching

```yaml
cache:
  redis:
    strategy:
      type: MULTI_LEVEL
```

### 2. REDIS_ONLY
**Best for**: Shared data that changes frequently

**Features:**
- Direct Redis access
- No local cache
- Always consistent across instances

**Use when:**
- Data changes frequently
- Consistency is critical
- You don't need ultra-low latency

```yaml
cache:
  redis:
    strategy:
      type: REDIS_ONLY
```

### 3. HASH
**Best for**: Structured data (user profiles, configs)

**Features:**
- Redis Hash operations
- Field-level access
- Efficient for structured data

**Use when:**
- You have structured data
- You need field-level operations
- You want to update individual fields

```yaml
cache:
  redis:
    strategy:
      type: HASH
```

### 4. UNIFIED
**Best for**: All features in one

**Features:**
- Multi-level caching
- Namespace support
- Stampede protection
- Circuit breaker
- All features built-in

**Use when:**
- You want everything
- You need namespaces
- You want the most features

```yaml
cache:
  redis:
    strategy:
      type: UNIFIED
```

## Usage Examples

### Example 1: Simple OTP Service (Current Implementation)

```java
@Service
@RequiredArgsConstructor
public class OtpServiceWithCacheClient implements OtpService {
    
    private final CacheClient cache; // Just inject this!
    
    @Override
    public void sendOtp(String mobile) {
        // Generate OTP with stampede protection
        String otp = cache.getOrCompute(
            "otp:" + mobile,
            String.class,
            this::generateOtp,
            Duration.ofMinutes(5)
        ).orElseThrow();
        
        // That's it! Strategy handles everything:
        // - L1/L2 caching (if MULTI_LEVEL)
        // - Compression (if enabled)
        // - Encryption (if enabled)
        // - Distributed eviction (if enabled)
    }
    
    @Override
    public void verifyOtp(String mobile, String otp) {
        Optional<String> cached = cache.get("otp:" + mobile, String.class);
        // Automatically checks L1 → L2 → miss
        
        if (cached.isPresent() && cached.get().equals(otp)) {
            cache.remove("otp:" + mobile);
            // Removes from L1 + L2 + notifies other instances
        }
    }
}
```

### Example 2: Switching Strategies

**Development (fast, local-only):**
```yaml
cache:
  redis:
    strategy:
      type: REDIS_ONLY  # Simple, no L1 cache
    compression:
      enabled: false
    encryption:
      enabled: false
```

**Production (high-performance):**
```yaml
cache:
  redis:
    strategy:
      type: MULTI_LEVEL  # L1 + L2 for performance
    compression:
      enabled: true  # Save bandwidth
    encryption:
      enabled: true  # Security
    eviction:
      enabled: true  # Cross-instance sync
```

**No code changes needed!** Just configuration.

### Example 3: Feature Composition

```yaml
# Scenario: High-security banking app
cache:
  redis:
    strategy:
      type: MULTI_LEVEL
    
    # Automatically applied as decorators
    compression:
      enabled: true  # Compress large values
    
    encryption:
      enabled: true  # Encrypt sensitive data
      algorithm: AES-256-GCM
      keyBase64: <your-key>
    
    eviction:
      enabled: true  # Sync across instances
```

**Result:** CacheClient automatically composes:
```
User Code
    ↓
CacheClient
    ↓
EncryptedDecorator (encrypts/decrypts)
    ↓
CompressingDecorator (compresses/decompresses)
    ↓
MultiLevelStrategy (L1 + L2 caching)
    ↓
Redis
```

## Benefits

### 1. Simplicity
```java
// Before: Choose from 8+ services
@Autowired private MultiLevelCacheService cache;
@Autowired private NamespacedCacheService cache;
@Autowired private CompressingCacheService cache;
// ... which one?

// After: One client
@Autowired private CacheClient cache;
```

### 2. Configuration-Driven
```yaml
# Change strategy without code changes
cache:
  redis:
    strategy:
      type: MULTI_LEVEL  # Change to REDIS_ONLY, HASH, etc.
```

### 3. Automatic Composition
```yaml
# Enable features via config
cache:
  redis:
    compression:
      enabled: true  # Automatically wraps with CompressingDecorator
    encryption:
      enabled: true  # Automatically wraps with EncryptedDecorator
```

### 4. Testability
```java
// Easy to mock
@MockBean
private CacheClient cache;

@Test
public void testOtpGeneration() {
    when(cache.getOrCompute(any(), any(), any(), any()))
        .thenReturn(Optional.of("123456"));
    
    otpService.sendOtp("+1234567890");
    
    verify(cache).getOrCompute(eq("otp:+1234567890"), any(), any(), any());
}
```

### 5. Performance
- No overhead - delegates directly to underlying services
- Same performance as using services directly
- Strategy selection at startup (no runtime overhead)

## Migration Guide

### Step 1: Update Dependencies
Already done - cache-starter includes CacheClient

### Step 2: Update Service Code

**Before:**
```java
@Autowired
private MultiLevelCacheService cacheService;
```

**After:**
```java
@Autowired
private CacheClient cache;
```

### Step 3: Update Configuration

Add to `application.yml`:
```yaml
cache:
  redis:
    strategy:
      type: MULTI_LEVEL  # or REDIS_ONLY, HASH, UNIFIED
```

### Step 4: Test

```bash
# Run OTP service
mvn spring-boot:run

# Test OTP generation
curl -X POST http://localhost:8080/api/v1/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"mobile": "+1234567890"}'

# Check logs for strategy
# Should see: "CacheClient initialized with strategy: MULTI_LEVEL"
```

## Comparison

| Aspect | Old Approach | New Approach (CacheClient) |
|--------|-------------|---------------------------|
| **Complexity** | 8+ services to choose from | 1 client |
| **Configuration** | Hardcoded in code | YAML-based |
| **Feature Composition** | Manual | Automatic |
| **Strategy Switching** | Code changes | Config changes |
| **Learning Curve** | High | Low |
| **Testability** | Complex mocking | Simple mocking |
| **Performance** | Same | Same |

## Conclusion

**CacheClient with Strategy Pattern provides:**
- ✅ Simple API (one client for everything)
- ✅ Configuration-driven (change strategy via YAML)
- ✅ Automatic composition (features applied as decorators)
- ✅ Easy testing (simple mocking)
- ✅ No performance overhead
- ✅ Backward compatible (old services still work)

**Recommendation:** Use **CacheClient** for all new code. Old services remain available for backward compatibility.

## Next Steps

1. ✅ CacheClient implemented
2. ✅ Strategy Pattern implemented
3. ✅ OTP service updated
4. ⏳ Test with different strategies
5. ⏳ Migrate other services
6. ⏳ Deprecate old direct service usage
