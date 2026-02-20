# AUTH PLATFORM -- REDIS DESIGN DOCUMENT

## Purpose of Redis in This System

Redis is used **ONLY for ephemeral, high-QPS, security-sensitive state**.

**Redis is NOT a database**
**Redis is NOT a source of truth**

> Redis exists to make the system **fast, safe, revocable, and resilient**.

---

## GLOBAL REDIS DESIGN PRINCIPLES

1. **Cluster-based only** (no single instance)
2. **Key TTL is mandatory**
3. **No cross-key transactions**
4. **Fail-closed for security paths**
5. **Clear ownership per cluster**
6. **Keys are namespace-isolated**
7. **Idempotency over retries**
8. **Bounded staleness accepted (AP)**

---

## REDIS CLUSTERS OVERVIEW

| Cluster             | Purpose          | Criticality |
|---------------------|------------------|-------------|
| Idempotency Cluster | Retry safety     | High        |
| OTP Cluster         | MFA security     | Critical    |
| Token Cluster       | Session control  | Critical    |
| Read Cache Cluster  | Performance      | Medium      |
| Rate Limit Cluster  | Abuse protection | High        |

Each cluster is **logically separate** (can be physically same Redis with prefix isolation, but shown separate at HLD).

---

# 1️⃣ IDEMPOTENCY CLUSTER

### Why it exists

* Prevent duplicate side effects
* Handle retries safely
* Ensure **exactly-once effect illusion**

### Key Pattern

```
idem:{service}:{endpoint}:{idempotency_key}
```

### Value

```json
{
  "status": 200,
  "response": "<base64-encoded-body>",
  "created_at": 1700000000
}
```

### TTL

```
1–5 minutes
```

### Flow

1. Gateway checks key
2. If exists → return cached response
3. If not → lock key + forward request
4. After success → store response

### Failure Rule

* Redis down → **fail closed** for auth endpoints

---

# 2️⃣ OTP CLUSTER (MFA — MOST SENSITIVE)

### Why it exists

* Store OTP temporarily
* Enforce single-use
* Enforce expiry

### Key Pattern

```
otp:{user_id}:{purpose}
```

### Value

```json
{
  "otp_hash": "<bcrypt/sha256>",
  "attempts": 0
}
```

### TTL

```
30–120 seconds
```

### Security Rules

* OTP stored **hashed**
* Increment `attempts`
* Max attempts (e.g. 3)
* On success → key deleted

### Failure Rule

* Redis unavailable → **authentication fails**
* Never fallback to DB

---

# 3️⃣ TOKEN CLUSTER (SESSION AUTHORITY)

### Why it exists

JWTs are stateless → **Redis adds revocation & control**

### Key Types

#### Refresh Token

```
rt:{token_id}
```

```json
{
  "user_id": "uuid",
  "device_id": "ios-123",
  "status": "ACTIVE"
}
```

TTL = refresh token lifetime (e.g. 30 days)

---

#### Access Token (Optional blacklist)

```
at:{jti}
```

```json
{
  "status": "REVOKED"
}
```

TTL = access token lifetime (e.g. 15 min)

---

### Used For

* Logout
* Forced logout
* Token refresh
* Device-level revocation

### Failure Rule

* Refresh/logout → fail closed
* Existing access tokens may continue until expiry

---

# 4️⃣ READ CACHE CLUSTER (PERFORMANCE ONLY)

### Why it exists

* Reduce DB reads
* Speed up token validation
* Reduce shard pressure

### Key Pattern

```
user:{user_id}
roles:{user_id}
perms:{user_id}
```

### Value

```json
{
  "user": {
    "..."
  },
  "roles": [
    "..."
  ],
  "permissions": [
    "..."
  ]
}
```

### TTL

```
30–300 seconds
```

### Invalidation

* On role change → delete keys
* On user update → delete keys

### Failure Rule

* Cache miss → DB fallback
* Cache down → DB fallback

---

# 5️⃣ RATE LIMIT CLUSTER (ABUSE PROTECTION)

### Why it exists

* Prevent brute force
* Prevent OTP abuse
* Prevent credential stuffing

### Key Patterns

#### Login attempts

```
rl:login:{ip}
rl:login:{user_id}
```

#### OTP attempts

```
rl:otp:{user_id}
```

### Value

```
counter
```

### TTL

```
60 seconds
```

### Enforcement

* Sliding window / token bucket
* Exceed → block request

---

## REDIS DATA THAT IS **NEVER STORED**

❌ Passwords
❌ Plain OTP
❌ Secrets
❌ JWT payloads
❌ Long-lived identity data

---

# REDIS VS DATABASE RESPONSIBILITY

| Concern     | Redis | DB |
|-------------|-------|----|
| Identity    | ❌     | ✅  |
| Sessions    | ✅     | ✅  |
| MFA         | ✅     | ❌  |
| Audits      | ❌     | ✅  |
| Rate limits | ✅     | ❌  |
| Revocation  | ✅     | ✅  |

Redis = **fast control plane**
DB = **source of truth**

---

# CAP THEOREM POSITIONING

| Cluster     | CAP Choice           |
|-------------|----------------------|
| Idempotency | AP (fail closed)     |
| OTP         | CP-like              |
| Token       | AP (security-biased) |
| Read Cache  | AP                   |
| Rate Limit  | AP                   |

---

# FAILURE SCENARIOS

| Failure                | Behavior               |
|------------------------|------------------------|
| OTP Redis down         | Login blocked          |
| Token Redis down       | Refresh blocked        |
| Cache Redis down       | DB fallback            |
| Rate-limit Redis down  | Fail closed or CAPTCHA |
| Idempotency Redis down | Auth blocked           |

