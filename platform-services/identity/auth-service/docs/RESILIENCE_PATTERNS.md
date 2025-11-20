# Resilience Patterns Implementation

This document describes the resilience patterns implemented in the auth-service to handle failures gracefully and maintain service availability.

## Overview

The auth-service implements the following resilience patterns using Resilience4j:

1. **Circuit Breaker** - Prevents cascading failures by stopping calls to failing services
2. **Retry** - Automatically retries failed operations with exponential backoff
3. **Timeout** - Limits the time spent waiting for responses (5 seconds max)
4. **Bulkhead** - Isolates thread pools to prevent resource exhaustion

## Requirements Addressed

- **5.1**: Circuit breaker pattern for external service calls (OTP service)
- **5.2**: Retry logic with exponential backoff for transient failures
- **5.3**: Timeout configuration for all external calls (5 seconds max)
- **5.4**: Bulkhead pattern for thread pool isolation

## Components

### 1. ResilienceConfig

Configuration class that sets up Resilience4j registries with custom settings:

- **Circuit Breaker Registry**: Configures circuit breakers for OTP service and database operations
- **Retry Registry**: Configures retry logic with exponential backoff
- **Time Limiter Registry**: Configures 5-second timeouts for external calls
- **Bulkhead Registry**: Configures thread pool isolation

Location: `com.immortals.authapp.config.ResilienceConfig`

### 2. OtpClient

Client for external OTP service with full resilience protection:

```java
@CircuitBreaker(name = "otpService", fallbackMethod = "sendOtpFallback")
@Retry(name = "otpService")
@TimeLimiter(name = "otpService")
@Bulkhead(name = "otpService")
public boolean sendOtp(String mobile)
```

Features:
- Circuit breaker opens after 50% failure rate
- Retries up to 3 times with exponential backoff (500ms, 1000ms, 2000ms)
- Times out after 5 seconds
- Limits concurrent calls to 10
- Provides fallback responses when circuit is open

Location: `com.immortals.authapp.client.OtpClientImpl`

### 3. ResilientDatabaseService

Service wrapper for database operations with retry and circuit breaker:

```java
@Retry(name = "databaseOperations")
@CircuitBreaker(name = "databaseOperations", fallbackMethod = "findUserFallback")
public Optional<User> findUserWithResilience(String userNameOrEmailOrPhone)
```

Features:
- Retries transient database failures (connection timeouts, deadlocks)
- Circuit breaker protects against database unavailability
- Provides safe fallback responses

Location: `com.immortals.authapp.service.ResilientDatabaseService`

### 4. ResilienceTestController

REST endpoints for testing and monitoring resilience patterns:

- `POST /api/v1/resilience/test/otp/send` - Test OTP service with resilience
- `GET /api/v1/resilience/test/database/user` - Test database operations
- `GET /api/v1/resilience/circuit-breakers` - View circuit breaker states
- `GET /api/v1/resilience/retries` - View retry statistics
- `POST /api/v1/resilience/circuit-breakers/{name}/reset` - Reset circuit breaker
- `POST /api/v1/resilience/circuit-breakers/{name}/open` - Open circuit breaker
- `POST /api/v1/resilience/circuit-breakers/{name}/close` - Close circuit breaker

Location: `com.immortals.authapp.controller.ResilienceTestController`

## Configuration

Resilience4j configuration is managed in the config-server at:
`config-server/src/main/resources/configurations/auth-service.yml`

### Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      otpService:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      databaseOperations:
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        failureRateThreshold: 60
        waitDurationInOpenState: 5s
```

### Retry Configuration

```yaml
resilience4j:
  retry:
    instances:
      otpService:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

### Timeout Configuration

```yaml
resilience4j:
  timelimiter:
    instances:
      otpService:
        timeoutDuration: 5s
        cancelRunningFuture: true
```

### Bulkhead Configuration

```yaml
resilience4j:
  bulkhead:
    instances:
      otpService:
        maxConcurrentCalls: 10
        maxWaitDuration: 500ms
      authenticationService:
        maxConcurrentCalls: 20
        maxWaitDuration: 1s
```

## Circuit Breaker States

1. **CLOSED** - Normal operation, all calls go through
2. **OPEN** - Circuit is open, calls fail immediately with fallback
3. **HALF_OPEN** - Testing if service has recovered, limited calls allowed

## Monitoring

### Health Indicators

Circuit breakers are registered with Spring Boot Actuator:

```bash
curl http://localhost:8083/actuator/health
```

### Metrics

Resilience4j metrics are exposed via Prometheus:

```bash
curl http://localhost:8083/actuator/prometheus | grep resilience4j
```

### Custom Endpoints

Monitor circuit breaker states:

```bash
curl http://localhost:8083/api/v1/resilience/circuit-breakers
```

View retry statistics:

```bash
curl http://localhost:8083/api/v1/resilience/retries
```

## Testing

### Test OTP Service Resilience

```bash
# Send OTP (will use circuit breaker, retry, timeout, bulkhead)
curl -X POST "http://localhost:8083/api/v1/resilience/test/otp/send?mobile=1234567890"
```

### Test Database Resilience

```bash
# Query user (will use retry and circuit breaker)
curl "http://localhost:8083/api/v1/resilience/test/database/user?username=testuser"
```

### Manually Control Circuit Breaker

```bash
# Open circuit breaker
curl -X POST http://localhost:8083/api/v1/resilience/circuit-breakers/otpService/open

# Close circuit breaker
curl -X POST http://localhost:8083/api/v1/resilience/circuit-breakers/otpService/close

# Reset circuit breaker
curl -X POST http://localhost:8083/api/v1/resilience/circuit-breakers/otpService/reset
```

## Best Practices

1. **Use appropriate timeouts**: 5 seconds for external services, shorter for internal operations
2. **Configure sensible retry attempts**: 3 attempts with exponential backoff
3. **Set realistic failure thresholds**: 50-60% failure rate before opening circuit
4. **Implement meaningful fallbacks**: Return cached data or default responses
5. **Monitor circuit breaker states**: Use actuator endpoints and metrics
6. **Test failure scenarios**: Use test endpoints to verify resilience behavior

## Fallback Strategies

### OTP Service Fallback

When OTP service is unavailable:
- Return false to indicate failure
- Log the error for monitoring
- Consider queuing the request for later processing
- Use alternative notification channels (email instead of SMS)

### Database Fallback

When database is unavailable:
- Return empty Optional for queries
- Throw exception for write operations (cannot proceed)
- Use cached data if available
- Implement read-only mode if possible

## Integration with Other Services

The resilience patterns can be applied to any external service call:

1. Create a client interface
2. Implement with `@CircuitBreaker`, `@Retry`, `@TimeLimiter`, `@Bulkhead` annotations
3. Provide fallback methods
4. Configure in `auth-service.yml`
5. Monitor via actuator endpoints

## Troubleshooting

### Circuit Breaker Stuck Open

If a circuit breaker remains open:
1. Check the service health
2. Review error logs
3. Manually reset using the reset endpoint
4. Adjust failure threshold if too sensitive

### Too Many Retries

If retries are excessive:
1. Reduce `maxAttempts`
2. Increase `waitDuration`
3. Review which exceptions trigger retries
4. Consider if operation is truly retriable

### Timeout Issues

If operations frequently timeout:
1. Increase `timeoutDuration` if appropriate
2. Optimize the slow operation
3. Consider async processing
4. Review network latency

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Resilience4j](https://resilience4j.readme.io/docs/getting-started-3)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- Requirements: 5.1, 5.2, 5.3, 5.4 in requirements.md
