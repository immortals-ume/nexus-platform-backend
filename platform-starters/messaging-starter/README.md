# Messaging Starter

📨 **Event-driven communication platform** for Spring Boot microservices using Apache Kafka. This starter provides a comprehensive messaging solution with automatic serialization, idempotency checking, retry logic, dead letter queue handling, and extensive observability features.

## 🌟 Key Features

### 🚀 Event Publishing
- **Simple API**: Easy-to-use event publishing with correlation ID propagation
- **Transactional Support**: Transactional event publishing with rollback capabilities
- **Async/Sync Modes**: Both asynchronous and synchronous publishing options
- **Batch Publishing**: Efficient batch event publishing for high throughput

### 🔄 Reliable Processing
- **Idempotency**: Automatic duplicate detection using Redis-based tracking
- **Retry Logic**: Configurable exponential backoff retry for transient failures
- **Dead Letter Queue**: Automatic handling of failed messages with manual retry support
- **Circuit Breaker**: Protection against cascade failures

### 📊 Observability
- **Comprehensive Metrics**: Built-in metrics for event processing and performance
- **Distributed Tracing**: Full tracing support with correlation ID propagation
- **Health Checks**: Kafka connectivity and consumer group health monitoring
- **Event Auditing**: Complete audit trail of event processing

### 🛡️ Resilience Patterns
- **Graceful Degradation**: Continues operation when messaging is unavailable
- **Backpressure Handling**: Intelligent handling of high message volumes
- **Consumer Scaling**: Dynamic consumer scaling based on lag
- **Error Classification**: Distinguishes between retryable and non-retryable errors

## 📦 Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>messaging-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## ⚙️ Configuration

### Basic Configuration

```yaml
platform:
  messaging:
    kafka:
      bootstrap-servers: localhost:9092
      consumer-group-prefix: my-service
      concurrency: 3
      auto-commit: false
      auto-offset-reset: earliest
      max-poll-records: 500
      session-timeout: 30s
      heartbeat-interval: 10s
    
    retry:
      enabled: true
      max-attempts: 3
      initial-interval: 1s
      max-interval: 30s
      multiplier: 2.0
    
    dlq:
      enabled: true
      topic-suffix: .dlq
      enable-manual-retry: true
      retention-time: 7d
    
    idempotency:
      enabled: true
      key-prefix: "messaging:idempotency:"
      ttl: 24h
```

## 💻 Usage Examples

### Publishing Events

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final EventPublisher eventPublisher;
    
    public void createUser(User user) {
        // Create user logic...
        
        // Publish event
        DomainEvent<UserCreatedPayload> event = DomainEvent.<UserCreatedPayload>builder()
            .eventType("UserCreated")
            .aggregateId(user.getId())
            .aggregateType("User")
            .payload(new UserCreatedPayload(user))
            .build();
        
        eventPublisher.publish("user-events", event);
    }
}
```

### Consuming Events

```java
@Component
@Slf4j
public class UserCreatedEventHandler extends DlqEnabledEventHandler<UserCreatedPayload> {
    
    public UserCreatedEventHandler() {
        initMetrics("UserCreatedEventHandler");
    }
    
    @Override
    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void handleEvent(
            @Payload DomainEvent<UserCreatedPayload> event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        super.handleEvent(event, topic, acknowledgment);
    }
    
    @Override
    protected void processEvent(DomainEvent<UserCreatedPayload> event) throws Exception {
        UserCreatedPayload payload = event.getPayload();
        log.info("Processing user created event for user: {}", payload.getUserId());
        
        // Process the event
        // This method will be retried automatically on failure
        // Failed events are automatically sent to DLQ
    }
    
    @Override
    protected String getEventType() {
        return "UserCreated";
    }
}
```

## 🔄 Dead Letter Queue Management

### Manual Retry from DLQ

```java
@Service
@RequiredArgsConstructor
public class DlqManagementService {
    
    private final DeadLetterQueueHandler dlqHandler;
    
    public void retryFailedEvent(DomainEvent<?> event, String originalTopic) {
        // Retry a failed event from DLQ
        dlqHandler.retryFromDeadLetterQueue(event, originalTopic);
    }
}
```

## 📊 Monitoring and Observability

### Available Metrics

```java
// Available metrics
event.handler.success{handler=UserCreatedEventHandler}     // Successful event processing
event.handler.failure{handler=UserCreatedEventHandler}     // Failed event processing
event.handler.duplicate{handler=UserCreatedEventHandler}   // Duplicate events detected
event.handler.processing.time{handler=UserCreatedEventHandler} // Processing time
```

### Health Checks

```bash
curl http://localhost:8080/actuator/health/kafka
```

## 🧪 Testing

### Unit Testing

```java
@SpringBootTest
class EventPublisherTest {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Test
    void shouldPublishEvent() {
        DomainEvent<TestPayload> event = DomainEvent.<TestPayload>builder()
            .eventType("TestEvent")
            .aggregateId("123")
            .payload(new TestPayload("test"))
            .build();
        
        eventPublisher.publish("test-topic", event);
        
        verify(kafkaTemplate).send(eq("test-topic"), any());
    }
}
```

### Integration Testing with Testcontainers

```java
@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );
    
    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.messaging.kafka.bootstrap-servers", 
            kafka::getBootstrapServers);
    }
    
    @Test
    void shouldPublishAndConsumeEvent() {
        // Test implementation
    }
}
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Events Not Being Consumed
- Check Kafka broker connectivity
- Verify consumer group configuration
- Check topic exists
- Review consumer logs

#### 2. Duplicate Events
- Verify idempotency is enabled
- Check Redis connectivity
- Review idempotency TTL settings

#### 3. DLQ Not Working
- Ensure `DlqEnabledEventHandler` is used
- Check DLQ topic exists
- Verify DLQ configuration

### Debug Configuration

```yaml
logging:
  level:
    com.immortals.platform.messaging: DEBUG
    org.springframework.kafka: DEBUG
```

## 📚 Best Practices

### 1. Event Design
- **Use meaningful event names** that reflect business events
- **Include all necessary data** in event payload
- **Version your events** for backward compatibility
- **Keep events immutable** once published

### 2. Handler Implementation
- **Extend DlqEnabledEventHandler** for automatic DLQ support
- **Implement idempotent processing** for all handlers
- **Use specific exception types** for retry logic
- **Log processing details** for debugging

### 3. Error Handling
- **Distinguish retryable vs non-retryable errors**
- **Implement circuit breakers** for external dependencies
- **Monitor DLQ sizes** and set up alerts
- **Have manual retry procedures** for DLQ messages

### 4. Performance
- **Use batch publishing** for high throughput
- **Optimize consumer concurrency** based on load
- **Monitor consumer lag** continuously
- **Implement backpressure handling**

### 5. Testing
- **Use Testcontainers** for integration tests
- **Mock external dependencies** in unit tests
- **Test error scenarios** and retry logic
- **Verify idempotency** in tests

## 📄 License

Copyright © 2024 Immortals Platform

Licensed under the Apache License, Version 2.0

## 🆘 Support

- 📖 **Documentation**: [Platform Starters Documentation](../README.md)
- 🐛 **Issues**: [GitHub Issues](https://github.com/YOUR_USERNAME/YOUR_REPO/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/YOUR_USERNAME/YOUR_REPO/discussions)
- 📧 **Email**: kapilsrivastava712@gmail.com

---

**Built with ❤️ by the Immortals Platform Team**