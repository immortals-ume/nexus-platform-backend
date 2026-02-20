# KAFKA TOPIC DESIGN

## 1️⃣ WHY KAFKA EXISTS IN THIS SYSTEM

Kafka is used **only for asynchronous, non-blocking side effects**.

### ❌ Kafka is NOT used for

* Authentication decision
* Authorization checks
* Token issuance
* Login success/failure response

### ✅ Kafka IS used for

* Audit logging
* Security analytics
* Fraud detection
* Notifications
* Downstream integrations

> **Rule:** Auth correctness must never depend on Kafka availability.

---

## 2️⃣ EVENTING PRINCIPLES (NON-NEGOTIABLE)

| Principle    | Rule                  |
|--------------|-----------------------|
| Delivery     | At-least-once         |
| Ordering     | Per user              |
| Schema       | Versioned             |
| Immutability | Events never updated  |
| Replay       | Always supported      |
| Failure      | Producers never block |

---

## 3️⃣ TOPIC NAMING CONVENTION

```
<domain>.<entity>.<event>.<version>
```

Example:

```
auth.user.login.v1
auth.user.registered.v1
```

Why:

* Clear ownership
* Evolvable
* Searchable
* Backward-compatible

---

## 4️⃣ CORE TOPICS

### 🔹 1. `auth.user.login.v1`

**Purpose**

* Successful login events (password / MFA / OAuth)

**Producers**

* Auth_Write_Service

**Consumers**

* Audit Service
* Analytics Service
* Fraud Service

**Key**

```text
user_uuid
```

**Partitions**

```
32 (scale with MAU)
```

---

### 🔹 2. `auth.user.login_failed.v1`

**Purpose**

* Failed login attempts

**Use cases**

* Brute-force detection
* Account lock logic
* SOC alerts

**Key**

```text
email OR user_uuid
```

---

### 🔹 3. `auth.user.mfa_challenge.v1`

**Purpose**

* MFA challenge initiated

**Consumers**

* Notification Service
* Security Analytics

---

### 🔹 4. `auth.user.mfa_verified.v1`

**Purpose**

* MFA success

**Guarantee**

* Emitted once per successful MFA

---

### 🔹 5. `auth.user.registered.v1`

**Purpose**

* New user registration

**Consumers**

* CRM
* Welcome email
* Analytics

---

### 🔹 6. `auth.user.password_reset_requested.v1`

**Purpose**

* Password reset initiated

---

### 🔹 7. `auth.user.password_changed.v1`

**Purpose**

* Password reset or change confirmed

**Security**

* Triggers session revocation

---

### 🔹 8. `auth.user.logged_out.v1`

**Purpose**

* Explicit logout

---

### 🔹 9. `auth.user.disabled.v1`

**Purpose**

* Admin disabled user

---

### 🔹 10. `auth.token.revoked.v1`

**Purpose**

* Session revocation (admin / reset)

---

### 🔹 11. `auth.security.anomaly.v1`

**Purpose**

* Fraud or suspicious behavior detected

**Producer**

* Fraud Service

---

## 5️⃣ EVENT PAYLOAD DESIGN (STANDARDIZED)

### 📦 Base Event Envelope (MANDATORY)

```json
{
  "event_id": "uuid",
  "event_type": "AUTH_USER_LOGIN",
  "event_version": 1,
  "occurred_at": "2026-01-04T18:30:00Z",

  "producer": "auth-write-service",

  "user": {
    "user_uuid": "uuid",
    "email": "user@example.com"
  },

  "request": {
    "request_id": "uuid",
    "idempotency_key": "uuid",
    "ip_address": "1.2.3.4",
    "user_agent": "Mozilla/5.0"
  },

  "payload": {}
}
```

---

## 6️⃣ SAMPLE EVENT PAYLOADS

### ✅ Successful Login

```json
{
  "event_type": "AUTH_USER_LOGIN",
  "payload": {
    "method": "PASSWORD",
    "mfa_used": true,
    "device_id": "ios-iphone-15"
  }
}
```

---

### ❌ Login Failed

```json
{
  "event_type": "AUTH_USER_LOGIN_FAILED",
  "payload": {
    "reason": "INVALID_PASSWORD",
    "failed_attempts": 3
  }
}
```

---

### 🔐 User Disabled

```json
{
  "event_type": "AUTH_USER_DISABLED",
  "payload": {
    "disabled_by": "admin_user_uuid",
    "reason": "POLICY_VIOLATION"
  }
}
```

---

## 7️⃣ PARTITIONING STRATEGY

| Topic          | Key               | Reason            |
|----------------|-------------------|-------------------|
| User events    | `user_uuid`       | Preserve ordering |
| Login failures | `email/user_uuid` | Lock accuracy     |
| Tokens         | `token_uuid`      | Session isolation |

---

## 8️⃣ PRODUCER GUARANTEES

### Producer settings (MANDATORY)

```properties
acks=all
enable.idempotence=true
retries=5
max.in.flight.requests.per.connection=5
```

Guarantees:

* No duplicates per producer session
* Safe retries
* Ordering per key

---

## 9️⃣ CONSUMER GUARANTEES

| Aspect      | Choice        |
|-------------|---------------|
| Commit      | Manual        |
| Processing  | At-least-once |
| Idempotency | Required      |
| Retry       | DLQ           |

---

## 🔁 DEAD LETTER TOPICS (DLQ)

```
auth.dlq.v1
```

Payload includes:

* Original event
* Failure reason
* Retry count

---

## 1️⃣0️⃣ SCHEMA EVOLUTION RULES

| Change             | Allowed |
|--------------------|---------|
| Add optional field | ✅       |
| Add new event type | ✅       |
| Remove field       | ❌       |
| Change meaning     | ❌       |

New version → new topic:

```
auth.user.login.v2
```

---

## 1️⃣1️⃣ SECURITY & COMPLIANCE

### ❌ DO NOT PUT

* Passwords
* OTPs
* Secrets
* Tokens

### ✅ ALLOWED

* UUIDs
* Hash references
* Metadata

---

## 1️⃣2️⃣ RETENTION POLICY

| Topic        | Retention |
|--------------|-----------|
| Audit        | 365 days  |
| Login failed | 90 days   |
| Security     | 180 days  |
| Analytics    | 30 days   |

---

## 1️⃣3️⃣ WHY THIS DESIGN IS CORRECT

✔ Auth is never blocked by Kafka
✔ Kafka scales independently
✔ Easy replay
✔ Security-safe
✔ Auditable
✔ Evolvable

