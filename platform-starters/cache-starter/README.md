Cache Starter — messy notes for an external dev

Short version (messy):
This README is intentionally messy-ish — think of it as the sticky-notes on my desk. If you're an external dev poking into `cache-starter`, you'll find a few "features" (annotations, compression, serialization, encryption), L1/L2 providers (Caffeine, Redis), and a MultiLevelCache that glues them together. The goal: split everything into small focused libraries (one feature per artifact) and keep `multilevel` separate too. Below I scribble what to extract, what to keep, tests to add, and rough priorities. No polished design doc — just enough to get you coding and extracting.

What this module contains (quick messy list):
- core: CacheService, NamespacedCacheService, DefaultUnifiedCacheManager, CacheStatistics
- providers: caffeine (L1), redis (L2), multilevel (glue)
- features: annotations (AOP + cacheable), compression, serialization, encryption
- observability hooks and properties

Why split things up:
- Each feature is conceptually independent (serialization, compression, encryption). Teams may want only some of them.
- Smaller artifacts = smaller transitive dependency footprint for downstream apps.
- MultiLevel should be a composition library that depends on providers and optional feature-libraries, not the other way around.

General extraction rules (apply to each feature):
- Minimal public API only: 1-2 interfaces + exceptions + one small configuration-properties object.
- Keep wiring (AutoConfiguration) in starter module only, but move the implementation classes into a feature lib.
- Provide a tiny compatibility facade in the current starter that simply re-exports the feature modules (thin adapter).

Per-feature messy extraction plan

1) Annotations (AOP: Cacheable, CachePut, CacheEvict, KeyGenerator, CacheAspect)
- What to create: artifactId `cache-feature-annotations`
- Package: `com.immortals.platform.cache.features.annotations` (same)
- Public API: annotations (`@Cacheable`, `@CachePut`, `@CacheEvict`), `KeyGenerator` interface, `ExpressionEvaluator` helper (if needed)
- Keep tests that validate annotation semantics (spans of behaviour): missing key, null key, TTL propagation
- Why: apps might want to use the annotations without pulling in compression/serialization code. Allows consumers to include only AOP and core.
- Priority: high
- Tests to add: annotation edge cases, expression parsing failures, thread-safety of KeyGenerator

2) Compression feature
- What to create: artifactId `cache-feature-compression`
- Package: `...features.compression`
- Public API: `CompressionStrategy`, `CompressionDecorator`, `GzipCompressionStrategy` (and exception class)
- Keep it small and pluggable; default impl = GZIP. Don't force a specific I/O lib.
- Priority: medium
- Tests: round-trip compress/decompress for small and large payloads, null values, malformed compressed payload

3) Serialization feature
- What to create: artifactId `cache-feature-serialization`
- Package: `...features.serialization`
- Public API: `SerializationStrategy`, `JacksonSerializationStrategy`, `JavaSerializationStrategy`, and `SerializationException`
- Rationale: serialization pulls in Jackson; make it optional. Default behavior in a separate lib prevents polluting core.
- Tests: custom POJOs, JSR-310 (dates), version-tolerant tests (missing fields), invalid payload handling
- Priority: high

4) Encryption feature
- What to create: artifactId `cache-feature-encryption`
- Package: `...features.encryption`
- Public API: `EncryptionStrategy`, `EncryptionDecorator`, `AesGcmEncryptionStrategy` (and exceptions)
- Security note: don't put keys in code. Provide hooks for passing keys (KeyProvider) and doc stub for KMS/secret storage.
- Tests: nonces/iv uniqueness, decrypt errors, tamper detection, integration with serialization/compression chain
- Priority: medium-high (security is important, but optional)

5) Observability feature (metrics/tracing)
- What to create: artifactId `cache-feature-observability` (or leave as part of common-starter if already present)
- Public API: small MeterRegistry adapter and decorators that record durations and counters
- Tests: ensure metrics counters increase on operations, test disabled metrics path
- Priority: low-medium

6) Providers (Caffeine L1, Redis L2)
- L1 (Caffeine)
  - artifactId `cache-provider-caffeine`
  - Public API: `L1CacheService` + `CaffeineProperties` only
  - Rationale: L1 is pure local, apps can bring it without redis
  - Tests: TTL behavior, max size eviction, concurrency micro-tests
  - Priority: high

- L2 (Redis / Redisson / Lettuce)
  - artifactId `cache-provider-redis`
  - Public API: `RedisCacheService` or `RedissonCacheService`, config properties
  - Rationale: L2 requires redis client deps — keep optional
  - Tests: integration tests using an embedded-redis or Testcontainers (integration)
  - Priority: high

7) Multilevel cache (composition)
- What to create: artifactId `cache-provider-multilevel`
- Purpose: compose L1 and L2 and optionally apply features (serialization, encryption, compression) as a configurable pipeline
- Public API: `MultiLevelCacheService<K,V>` (simple constructor/factory that accepts L1, L2, optional EvictionPublisher, namespace, and a chain builder), plus `EvictionPublisher` interface
- Responsibilities: read-through with L1-first, populate L1 on L2 hit, write-through to both, fallbacks (when L2 unavailable) — keep the error-handling/results documented
- Tests: unit tests mocking L1/L2, plus a small integration test (L1=caffeine, L2=redis) — run these via profile/testcontainers
- Priority: high

How to simplify each feature/library (concrete suggestions)
- Keep only one interface per concern. For example, `SerializationStrategy` is the only public type; implementations live in the feature module.
- Keep exception types in the feature module so the starter doesn't need to map them.
- Keep AutoConfiguration classes in the starter module (or a `cache-autoconfigure` artifact) that wires optional modules when on classpath.
- Avoid cross-feature dependencies: decorators should be composable and small (accept and return bytes or objects consistently) so they chain.

Developer checklist (chaotic):
- [ ] Extract `features/serialization` -> cache-feature-serialization module
- [ ] Extract `features/compression` -> cache-feature-compression module
- [ ] Extract `features/encryption` -> cache-feature-encryption module
- [ ] Extract `features/annotations` -> cache-feature-annotations module
- [ ] Extract `providers/caffeine` -> cache-provider-caffeine module
- [ ] Extract `providers/redis` -> cache-provider-redis module (optional)
- [ ] Extract `providers/multilevel` -> cache-provider-multilevel module
- [ ] Keep `core` in `cache-core` artifact (CacheService, NamespacedCacheService, CacheStatistics)
- [ ] Create a small `cache-starter` artifact (auto-config) that depends on the optional modules via optional dependencies or BOM
- [ ] Add README and quick examples in each extracted module

Minimal API contracts (messy but practical)
- CacheService<K,V> — keep as-is (put/get/remove/clear/putIfAbsent/increment/decrement/getAll/putAll)
- SerializationStrategy
  - byte[] serialize(Object)
  - T deserialize(byte[], Class<T>)
- CompressionStrategy
  - byte[] compress(byte[])
  - byte[] decompress(byte[])
- EncryptionStrategy
  - byte[] encrypt(byte[])
  - byte[] decrypt(byte[])
- EvictionPublisher
  - publishKeyEviction(String namespace, String key)
  - publishClearAll(String namespace)

Quick dev commands (from repository root) — run module tests:

```bash
# run only the cache-starter module tests
mvn -pl platform-starters/cache-starter -am test

# run a single test class
mvn -Dtest=com.immortals.platform.cache.providers.multilevel.MultiLevelCacheServiceTest -pl platform-starters/cache-starter test
```

Notes, caveats and messy advice:
- Don’t tightly couple decorators to one another. Prefer that decorators operate on a clear data shape (e.g., raw bytes) where possible, or build small adapters.
- Keep AutoConfiguration lazy and conditional on classes or properties to avoid pulling optional deps into apps.
- For security-sensitive modules (encryption), document how to supply keys and rotate them; add a minimal KeyProvider interface and treat the default as a NOOP that throws if misconfigured.
- For L2/redis testing prefer Testcontainers in CI; for local quick dev, provide an embedded-redis setup or a small test configuration.

Next steps (fast and dirty):
1. Pick one feature (annotations or serialization) and extract it as a separate Maven module with a narrow public API and tests.
2. Create a new `cache-core` module with `CacheService` and `NamespacedCacheService` and make other modules depend on it.
3. Turn `multilevel` into a composition library that accepts any `CacheService` implementations and optional decorators.
4. Add clear README + example for each new module.

If you want I can:
- create skeleton pom.xml and module structure for one feature now (e.g., `cache-feature-serialization`) and wire it in the multi-module pom; or
- scaffold a simpler `cache-provider-multilevel` README + small example showing how to build a MultiLevelCacheService from L1 and L2

— end of messy notes —

If this README is too neat, say so and I’ll make it look worse.
