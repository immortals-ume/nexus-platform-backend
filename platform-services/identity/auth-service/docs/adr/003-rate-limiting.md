# ADR 003: Rate Limiting Implementation

## Status

Accepted

## Context

The Auth App needs protection against abuse, such as brute force attacks, denial of service attacks, and excessive API
usage. We need a rate limiting solution that:

1. Protects authentication endpoints from brute force attacks
2. Prevents denial of service attacks
3. Ensures fair resource allocation among users
4. Scales in a distributed environment
5. Has minimal performance impact
6. Provides flexibility for different rate limits based on endpoint sensitivity

## Decision

We will implement a Redis-based rate limiting solution with the following characteristics:

1. **Implementation Approach**:
    - Use a token bucket algorithm for rate limiting
    - Implement as a servlet filter that executes early in the request processing chain
    - Use Redis for distributed rate limit tracking

2. **Rate Limit Identifiers**:
    - IP address-based rate limiting for all requests
    - Username-based rate limiting for authentication attempts
    - Combined IP and path-based rate limiting for sensitive endpoints

3. **Rate Limit Policies**:
    - Global rate limits for all API endpoints
    - Stricter rate limits for authentication endpoints
    - Graduated blocking for repeated authentication failures
    - Configurable rate limits per endpoint or endpoint group

4. **Response Handling**:
    - Return HTTP 429 (Too Many Requests) when rate limit is exceeded
    - Include rate limit headers in responses:
        - `X-RateLimit-Limit`: The maximum number of requests allowed in the time window
        - `X-RateLimit-Remaining`: The number of requests remaining in the current time window
        - `X-RateLimit-Reset`: The time when the current rate limit window resets

5. **Monitoring and Alerting**:
    - Log rate limit events for monitoring
    - Generate alerts for suspicious patterns (e.g., multiple IPs hitting rate limits)

## Consequences

### Advantages

1. **Security**: Protects against brute force and DoS attacks
2. **Resource Protection**: Prevents excessive resource consumption by individual users
3. **Fairness**: Ensures fair resource allocation among all users
4. **Scalability**: Redis-based implementation works in distributed environments
5. **Flexibility**: Different rate limits can be applied to different endpoints or users

### Disadvantages

1. **Complexity**: Adds complexity to the request processing pipeline
2. **Redis Dependency**: Creates a dependency on Redis for rate limit tracking
3. **False Positives**: Legitimate users behind shared IPs might be affected
4. **Performance Impact**: Adds a small overhead to each request

### Mitigations

1. **Complexity**: Encapsulate rate limiting logic in a dedicated service
2. **Redis Dependency**: Implement fallback mechanisms for Redis unavailability
3. **False Positives**: Implement whitelist mechanisms for trusted IPs
4. **Performance Impact**: Optimize Redis operations and consider local caching

## Alternatives Considered

1. **In-Memory Rate Limiting**:
    - Pros: Simpler implementation, no external dependencies
    - Cons: Doesn't work in distributed environments, memory limitations

2. **Database-Based Rate Limiting**:
    - Pros: Persistent storage, works in distributed environments
    - Cons: Higher latency, potential database performance impact

3. **API Gateway Rate Limiting**:
    - Pros: Offloads rate limiting to the API gateway
    - Cons: Less control over implementation details, may not support all required features

4. **Third-Party Rate Limiting Service**:
    - Pros: Managed solution, potentially more sophisticated
    - Cons: External dependency, potential cost, integration complexity

## Implementation Details

The rate limiting is implemented in the `RateLimitFilter` class, which extends Spring's `OncePerRequestFilter`. The
filter:

1. Extracts the client's IP address from the request
2. Checks with the `RateLimiterService` if the request is allowed
3. If allowed, passes the request to the next filter in the chain
4. If not allowed, returns a 429 Too Many Requests response

The `RateLimiterService` uses Redis to track request counts and enforce rate limits.

## References

- [OWASP API Security Top 10: API4:2019 - Lack of Resources & Rate Limiting](https://owasp.org/www-project-api-security/)
- [Redis Rate Limiting Pattern](https://redis.io/commands/incr#pattern-rate-limiter)
- [Spring Boot Rate Limiting](https://www.baeldung.com/spring-bucket4j)