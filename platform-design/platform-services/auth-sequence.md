# Login and Authentication Sequences

## Purpose

Authenticate a user using **username/password** and optionally **MFA (OTP)**, ensuring retries are safe and no duplicate
side effects occur.

## Preconditions

* User exists
* User account is active
* Idempotency key is provided by client

## Sequence: Login + MFA Initiation

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Idem as Idempotency(Redis)
    participant Orch as Auth_Orchestrator
    participant Write as Auth_Write_Service
    participant DB as User_DB_Shard
    participant OTP as OTP_Service
    participant Cache as OTP_Redis
    Client ->> Gateway: POST /auth/login
    Gateway ->> Idem: check(idempotency-key)
    Idem -->> Gateway: allow
    Gateway ->> Orch: login()
    Orch ->> Write: authenticate()
    Write ->> DB: verify credentials
    DB -->> Write: valid
    Write ->> OTP: generate OTP
    OTP ->> Cache: store OTP (TTL)
    OTP -->> Client: send OTP
    Write -->> Orch: MFA_REQUIRED
    Orch -->> Gateway: MFA_REQUIRED
    Gateway -->> Client: Enter OTP
```

## Guarantees

* Password is verified only once per idempotency key
* OTP is short-lived and stored only in Redis
* Login fails closed on DB or OTP failure

---

# Sequence: OTP Verification

## Purpose

Verify OTP and complete authentication by issuing tokens.

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch
    participant Write
    participant Cache as OTP_Redis
    Client ->> Gateway: POST /auth/mfa/verify
    Gateway ->> Orch: verifyOTP()
    Orch ->> Write: verifyOTP()
    Write ->> Cache: validate OTP
    Cache -->> Write: valid
    Write -->> Orch: success
    Orch -->> Gateway: JWT + Refresh Token
    Gateway -->> Client: Login Successful
```

## Guarantees

* OTP can be used only once
* Expired or reused OTP is rejected
* Tokens are issued only after MFA success

---

# Sequence: Registration-sequence

## Purpose

Create a new user account in the correct shard and emit audit events.

## Preconditions

* Email / phone not already registered
* Valid registration payload

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch as Auth_Orchestrator
    participant Write as Auth_Write_Service
    participant Router as Shard_Router
    participant DB as User_DB_Shard
    participant Kafka
    Client ->> Gateway: POST /auth/register
    Gateway ->> Orch: register()
    Orch ->> Write: createUser()
    Write ->> Router: resolve shard
    Router ->> DB: insert user
    DB -->> Write: created
    Write ->> Kafka: UserRegistered
    Write -->> Orch: success
    Orch -->> Gateway: 201 Created
    Gateway -->> Client: Registration Successful
```

## Guarantees

* User is written to exactly one shard
* No cross-shard writes
* Registration event is emitted asynchronously

---

# Sequence: OAuth-sequence

## Purpose

Authenticate users via external identity providers (Google, Apple, SSO).

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch
    participant Write
    participant CB as CircuitBreaker
    participant OAuth
    participant DB
    Client ->> Gateway: GET /auth/oauth/callback
    Gateway ->> Orch: oauthLogin()
    Orch ->> Write: exchangeCode()
    Write ->> CB: call OAuth
    CB ->> OAuth: exchange code
    OAuth -->> CB: user profile
    CB -->> Write: success
    Write ->> DB: upsert user
    Write -->> Orch: JWT + Refresh
    Orch -->> Gateway: success
    Gateway -->> Client: Logged In
```

## Guarantees

* OAuth failures do not cascade (circuit breaker)
* User record is idempotently created or updated
* External calls are never on read path

---

# Sequence: token-refresh

## Purpose

Issue a new access token using a valid refresh token.

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch
    participant Read as Auth_Read_Service
    participant Token as Token_Redis
    Client ->> Gateway: POST /auth/refresh
    Gateway ->> Orch: refresh()
    Orch ->> Read: validateRefreshToken()
    Read ->> Token: lookup token
    Token -->> Read: valid
    Read -->> Orch: issue new JWT
    Orch -->> Gateway: new JWT
    Gateway -->> Client: Token Refreshed
```

## Guarantees

* Refresh tokens are centrally revocable
* Access tokens remain short-lived
* No DB access on refresh path

---

# Sequence: logout-sequence

## Purpose

Invalidate user sessions and revoke refresh tokens.

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch
    participant Read
    participant Token as Token_Redis
    participant Kafka
    Client ->> Gateway: POST /auth/logout
    Gateway ->> Orch: logout()
    Orch ->> Read: revoke token
    Read ->> Token: delete refresh token
    Read ->> Kafka: UserLoggedOut
    Orch -->> Gateway: success
    Gateway -->> Client: Logged Out
```

## Guarantees

* Logout is idempotent
* Tokens cannot be reused after logout
* Audit event is always emitted

---

# Sequence: token validation

## Purpose

Validate JWT for every API request with minimal latency.

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Cache as Token_Redis
    participant Read as Auth_Read_Service
    Client ->> Gateway: API Request (JWT)
    Gateway ->> Cache: check token
    Cache -->> Gateway: hit

    alt cache miss
        Gateway ->> Read: validate token
        Read -->> Gateway: valid
        Gateway ->> Cache: backfill
    end

    Gateway -->> Client: Request Allowed
```

## Guarantees

* High availability (AP)
* Bounded staleness accepted
* Gateway enforces authorization decisions
---

## ➕ Sequence: Password Reset (Request)

### Purpose

Initiate secure password recovery.

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch
    participant Write
    participant DB
    participant Kafka
    Client ->> Gateway: POST /auth/password/reset/request
    Gateway ->> Orch: resetRequest()
    Orch ->> Write: createResetToken()
    Write ->> DB: store reset_token_hash
    Write ->> Kafka: PasswordResetRequested
    Orch -->> Gateway: 202 Accepted
    Gateway -->> Client: Reset Email Sent
```

### Guarantees

* Token is single-use
* Token expires
* Event always audited

---

## ➕ Sequence: Password Reset (Confirm)

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch
    participant Write
    participant DB
    Client ->> Gateway: POST /auth/password/reset/confirm
    Gateway ->> Orch: confirmReset()
    Orch ->> Write: validate token
    Write ->> DB: update password
    Write ->> DB: invalidate reset token
    Orch -->> Gateway: success
    Gateway -->> Client: Password Updated
```

---

## ➕ Sequence: Account Lock (Brute Force)

### Purpose

Lock account after repeated failures.

```mermaid
sequenceDiagram
    participant Gateway
    participant Orch
    participant Write
    participant DB
    participant Kafka
    Gateway ->> Orch: login failure
    Orch ->> Write: increment failures
    Write ->> DB: update failed_attempts

    alt threshold exceeded
        Write ->> DB: lock account
        Write ->> Kafka: AccountLocked
    end
```

---

## ➕ Sequence: Admin User Disable (Force Logout)

### Purpose

Immediately revoke all user sessions.

```mermaid
sequenceDiagram
    participant Admin
    participant Gateway
    participant Orch
    participant Write
    participant Token as Token_Redis
    participant Kafka
    Admin ->> Gateway: POST /admin/users/disable
    Gateway ->> Orch: disableUser()
    Orch ->> Write: update status
    Write ->> Token: revoke all tokens
    Write ->> Kafka: UserDisabled
    Orch -->> Gateway: success
```

### Guarantees

* All refresh tokens invalidated
* Access tokens expire naturally
* No further login allowed

---

## ➕ Sequence: New Device Login (MFA Escalation)

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Orch
    participant Write
    participant DB
    participant OTP
    Client ->> Gateway: POST /auth/login
    Gateway ->> Orch: login()
    Orch ->> Write: detect device
    Write ->> DB: check known device

    alt new device
        Write ->> OTP: force MFA
        OTP -->> Client: send OTP
    end
```

---

## ➕ Sequence: Auth Read — User Context

### Purpose

Used by downstream services.

```mermaid
sequenceDiagram
    participant Service
    participant Gateway
    participant Read
    participant Cache
    participant DB
    Service ->> Gateway: GET /auth/me
    Gateway ->> Read: getUserContext()
    Read ->> Cache: lookup
    Cache -->> Read: hit

    alt cache miss
        Read ->> DB: fetch user + roles
        Read ->> Cache: backfill
    end

    Read -->> Gateway: user context
    Gateway -->> Service: response
```


