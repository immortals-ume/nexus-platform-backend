# Messaging Starter

A Spring Boot starter that provides event-driven communication capabilities using Apache Kafka. This starter includes automatic serialization/deserialization, idempotency checking, retry logic with exponential backoff, dead letter queue handling, and comprehensive metrics.

## Features

- **Kafka Auto-Configuration**: Automatic setup of Kafka producers and consumers with JSON serialization
- **Event Publishing**: Simple API for publishing domain events with correlation ID propagation
- **Idempotency**: Automatic duplicate detection using Redis
- **Retry Logic**: Exponential backoff retry for transient failures
- **Dead Letter Queue**: Automatic handling of failed messages
- **Metrics**: Built-in metrics for event processing
- **Transactional Support**: Transactional event publishing

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.immortals.platform</groupId>
    <artifactId>messaging-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

Configure the messaging properties in your `application.yml`:

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

## Usage

### Publishing Events

Inject the `EventPublisher` and publish domain events:

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

Extend `AbstractEventHandler` to create event handlers:

```java
@Component
@Slf4j
public class UserCreatedEventHandler extends AbstractEventHandler<UserCreatedPayload> {
    
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
    }
    
    @Override
    protected String getEventType() {
        return "UserCreated";
    }
}
```

### Synchronous Publishing

For transactional scenarios, use synchronous publishing:

```java
@Transactional
public void createOrder(Order order) {
    // Save order to database
    orderRepository.save(order);
    
    // Publish event synchronously (will rollback if publishing fails)
    DomainEvent<OrderCreatedPayload> event = DomainEvent.<OrderCreatedPayload>builder()
        .eventType("OrderCreated")
        .aggregateId(order.getId())
        .aggregateType("Order")
        .payload(new OrderCreatedPayload(order))
        .build();
    
    eventPublisher.publishSync("order-events", event);
}
```

## Features in Detail

### Idempotency

The starter automatically tracks processed events in Redis to prevent duplicate processing:

- Each event is identified by its `eventId`
- Processed events are stored in Redis with a configurable TTL
- Duplicate events are automatically skipped

### Retry Logic

Failed event processing is automatically retried with exponential backoff:

- Configurable maximum retry attempts
- Exponential backoff with configurable multiplier
- Maximum backoff interval to prevent excessive delays

### Dead Letter Queue

Events that fail after all retries are sent to a dead letter queue:

- DLQ topic is automatically created with `.dlq` suffix
- Failed events include error metadata
- Manual retry support for persistent failures

### Metrics

The following metrics are automatically collected:

- `event.handler.success`: Number of successfully processed events
- `event.handler.failure`: Number of failed event processing attempts
- `event.handler.duplicate`: Number of duplicate events detected
- `event.handler.processing.time`: Time taken to process events

### Correlation ID Propagation

Correlation IDs are automatically propagated from the current context to published events, enabling distributed tracing across services.

## Requirements

- Spring Boot 3.x
- Apache Kafka 3.x
- Redis (for idempotency checking)
- Java 17+

## Dependencies

This starter depends on:

- `observability-starter`: For distributed tracing and metrics
- `common-starter`: For common utilities and exception handling

## License

Copyright © 2024 Immortals Platform
