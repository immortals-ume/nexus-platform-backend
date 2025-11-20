# ADR 001: JWT-Based Authentication

## Status

Accepted

## Context

The Auth App requires a secure, scalable, and stateless authentication mechanism that can work effectively in a
microservices architecture. We need to choose an authentication approach that:

1. Supports stateless authentication to avoid session management across distributed services
2. Provides secure transmission of authentication information
3. Allows for fine-grained access control
4. Scales horizontally without shared session state
5. Supports token expiration and revocation
6. Can be validated by multiple services independently

## Decision

We will implement JSON Web Token (JWT) based authentication with the following characteristics:

1. **Token Structure**:
    - Header: Algorithm and token type
    - Payload: User identity, roles, permissions, and metadata
    - Signature: Cryptographically signed with a secret key

2. **Token Management**:
    - Short-lived access tokens (1 hour expiration)
    - Longer-lived refresh tokens (24 hours expiration)
    - Refresh tokens stored securely as HTTP-only cookies
    - Access tokens transmitted in Authorization header

3. **Security Measures**:
    - Tokens signed with HMAC SHA-256 (HS256)
    - Tokens include expiration time (exp), issued at time (iat), and not before time (nbf)
    - Tokens include audience (aud) and issuer (iss) claims for validation
    - Token blacklisting for revoked tokens using Redis

4. **Implementation**:
    - Use the `jjwt` library for JWT creation and validation
    - Implement custom filters in Spring Security for JWT processing
    - Use Spring Security's authentication providers for user authentication

## Consequences

### Advantages

1. **Stateless Authentication**: No need to store session information on the server, making horizontal scaling easier.
2. **Cross-Domain Support**: JWTs can be used across different domains and services.
3. **Performance**: Token validation is fast and doesn't require database lookups for each request.
4. **Security**: Tokens are cryptographically signed and can include expiration times.
5. **Flexibility**: Tokens can carry additional user information and permissions.

### Disadvantages

1. **Token Size**: JWTs can be larger than session IDs, increasing the payload size of each request.
2. **Token Revocation**: Immediate token revocation requires additional mechanisms like a token blacklist.
3. **Secret Management**: The signing secret must be securely managed and rotated periodically.
4. **Token Exposure**: If a token is compromised, it can be used until it expires or is blacklisted.

### Mitigations

1. **Token Size**: Keep the payload minimal, including only necessary claims.
2. **Token Revocation**: Implement a Redis-based token blacklist for immediate revocation.
3. **Secret Management**: Use environment variables and secure vaults for secret storage.
4. **Token Exposure**: Use short-lived access tokens and secure transmission via HTTPS.

## Alternatives Considered

1. **Session-Based Authentication**:
    - Pros: Simpler to implement, easier to revoke
    - Cons: Requires session state management, doesn't scale as well in distributed systems

2. **OAuth 2.0 with External Provider**:
    - Pros: Delegates authentication responsibility, industry standard
    - Cons: More complex to implement, external dependency

3. **API Keys**:
    - Pros: Simple to implement
    - Cons: Limited security, no built-in expiration, no standard structure

## References

- [JWT.io](https://jwt.io/)
- [RFC 7519 - JSON Web Token](https://tools.ietf.org/html/rfc7519)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)