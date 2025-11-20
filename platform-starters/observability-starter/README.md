# Observability Starter

A comprehensive Spring Boot starter for observability features including distributed tracing, metrics collection, and structured logging.

## Features

### Distributed Tracing
- **Micrometer Tracing** with OpenTelemetry bridge
- **W3C Trace Context** propagation across HTTP boundaries
- **Zipkin** exporter for trace visualization
- Configurable sampling rate (default 10%)
- Automatic trace context injection

### Metrics Collection
- **Prometheus** metrics export at `/actuator/prometheus`
- JVM metrics (memory, GC, threads, class loader)
- System metrics (CPU, uptime)
- Custom business metrics support
- Micrometer registry integration

### Structured Logging
- **JSON structured logging** using Logstash encoder
- **Correlation ID** tracking across requests
- Automatic inclusion of trace ID and span ID in logs
- Request/response logging interceptor (optional)
- MDC (Mapped Diagnostic Context) support

## Usage

### 1. Add Dependency

Add the starter to your `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>observability-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configuration

Configure in your `application.yml`:

```yaml
platform:
  observability:
    tracing:
      enabled: true
      sampling-rate: 0.1  # 10% sampling
      exporter: zipkin
      zipkin-url: http://localhost:9411
    metrics:
      enabled: true
      export-interval: 60
    logging:
      format: json  # or 'text'
      level: INFO
      request-response-logging: true

spring:
  application:
    name: my-service  # Required for tracing
```

### 3. Using Custom Metrics

Inject `CustomMetricsService` to record business metrics:

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final CustomMetricsService metricsService;
    
    public void processOrder(Order order) {
        // Increment counter
        metricsService.incrementCounter("orders.processed", 
            "status", order.getStatus());
        
        // Time an operation
        metricsService.timeRunnable("orders.processing.time", () -> {
            // Your business logic
        }, "type", "standard");
    }
}
```

### 4. Correlation ID

Correlation IDs are automatically added to:
- HTTP response headers (`X-Correlation-ID`)
- Log entries (MDC key: `correlationId`)
- Trace context

To propagate correlation IDs to downstream services:

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final RestTemplate restTemplate;
    
    public void callDownstream() {
        String correlationId = MDC.get("correlationId");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", correlationId);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }
}
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `platform.observability.tracing.enabled` | `true` | Enable/disable distributed tracing |
| `platform.observability.tracing.sampling-rate` | `0.1` | Trace sampling rate (0.0 to 1.0) |
| `platform.observability.tracing.exporter` | `zipkin` | Trace exporter type |
| `platform.observability.tracing.zipkin-url` | `http://localhost:9411` | Zipkin server URL |
| `platform.observability.metrics.enabled` | `true` | Enable/disable metrics collection |
| `platform.observability.metrics.export-interval` | `60` | Metrics export interval in seconds |
| `platform.observability.logging.format` | `json` | Log format (json or text) |
| `platform.observability.logging.level` | `INFO` | Default log level |
| `platform.observability.logging.request-response-logging` | `false` | Enable request/response logging |

## Actuator Endpoints

The starter automatically exposes the following actuator endpoints:

- `/actuator/health` - Health check
- `/actuator/info` - Application info
- `/actuator/metrics` - Available metrics
- `/actuator/prometheus` - Prometheus metrics export

## Log Format

### JSON Format (Default)

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.example.MyService",
  "message": "Processing order",
  "correlationId": "abc-123-def",
  "traceId": "1234567890abcdef",
  "spanId": "fedcba0987654321",
  "service": "my-service"
}
```

### Text Format

```
2024-01-15 10:30:00.123 [http-nio-8080-exec-1] INFO  com.example.MyService [correlationId=abc-123-def] [traceId=1234567890abcdef] [spanId=fedcba0987654321] - Processing order
```

## Requirements

- Java 17+
- Spring Boot 3.5.4+
- Spring Cloud 2024.0.1+

## Dependencies

This starter includes:
- Micrometer Tracing with OpenTelemetry
- Micrometer Prometheus Registry
- Zipkin Reporter
- Logstash Logback Encoder
- Spring Boot Actuator

## Integration with Other Services

### Zipkin

Run Zipkin locally:

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

Access Zipkin UI at: http://localhost:9411

### Prometheus

Configure Prometheus to scrape metrics:

```yaml
scrape_configs:
  - job_name: 'my-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

## Best Practices

1. **Always set `spring.application.name`** - Required for proper service identification in traces
2. **Use appropriate sampling rates** - Higher rates in dev/staging, lower in production
3. **Propagate correlation IDs** - Include in all downstream service calls
4. **Use custom metrics wisely** - Focus on business-critical operations
5. **Monitor actuator endpoints** - Ensure they're accessible to monitoring tools

## Troubleshooting

### Traces not appearing in Zipkin

- Verify Zipkin URL is correct
- Check sampling rate (increase for testing)
- Ensure `spring.application.name` is set
- Check network connectivity to Zipkin

### Metrics not exported

- Verify `/actuator/prometheus` endpoint is accessible
- Check `platform.observability.metrics.enabled=true`
- Ensure Prometheus is configured to scrape the endpoint

### Logs not in JSON format

- Verify `platform.observability.logging.format=json`
- Check for conflicting Logback configurations
- Ensure Logstash encoder dependency is present

## License

Copyright © 2024 Immortals Platform
