package com.immortals.platform.messaging.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for DomainEvent.
 */
class DomainEventTest {

    @Test
    void shouldCreateDomainEventWithDefaults() {
        DomainEvent<String> event = new DomainEvent<>();
        
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getMetadata()).isNotNull().isEmpty();
        assertThat(event.getVersion()).isEqualTo("1.0");
    }

    @Test
    void shouldCreateDomainEventWithAllParameters() {
        String eventId = "test-event-id";
        String eventType = "TestEvent";
        String aggregateId = "test-aggregate-id";
        String aggregateType = "TestAggregate";
        String payload = "test-payload";
        Instant timestamp = Instant.now();
        String correlationId = "test-correlation-id";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        String version = "2.0";
        String source = "test-service";

        DomainEvent<String> event = new DomainEvent<>(
                eventId, eventType, aggregateId, aggregateType, payload,
                timestamp, correlationId, metadata, version, source
        );

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getAggregateType()).isEqualTo(aggregateType);
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getMetadata()).isEqualTo(metadata);
        assertThat(event.getVersion()).isEqualTo(version);
        assertThat(event.getSource()).isEqualTo(source);
    }

    @Test
    void shouldCreateDomainEventUsingBuilder() {
        String eventType = "UserCreated";
        String aggregateId = "user-123";
        String payload = "user-data";
        String correlationId = "correlation-123";
        String source = "user-service";

        DomainEvent<String> event = DomainEvent.<String>builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .aggregateType("User")
                .payload(payload)
                .correlationId(correlationId)
                .source(source)
                .version("2.0")
                .build();

        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getAggregateType()).isEqualTo("User");
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getVersion()).isEqualTo("2.0");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getMetadata()).isNotNull();
    }

    @Test
    void shouldAddMetadata() {
        DomainEvent<String> event = new DomainEvent<>();
        
        event.addMetadata("key1", "value1");
        event.addMetadata("key2", "value2");
        
        assertThat(event.getMetadata()).hasSize(2);
        assertThat(event.getMetadata()).containsEntry("key1", "value1");
        assertThat(event.getMetadata()).containsEntry("key2", "value2");
    }

    @Test
    void shouldGetMetadata() {
        DomainEvent<String> event = new DomainEvent<>();
        event.addMetadata("testKey", "testValue");
        
        String value = event.getMetadata("testKey");
        String nonExistentValue = event.getMetadata("nonExistent");
        
        assertThat(value).isEqualTo("testValue");
        assertThat(nonExistentValue).isNull();
    }

    @Test
    void shouldHandleNullMetadataMap() {
        DomainEvent<String> event = new DomainEvent<>();
        event.setMetadata(null);
        
        event.addMetadata("key", "value");
        
        assertThat(event.getMetadata()).isNotNull();
        assertThat(event.getMetadata()).containsEntry("key", "value");
    }

    @Test
    void shouldGetMetadataFromNullMap() {
        DomainEvent<String> event = new DomainEvent<>();
        event.setMetadata(null);
        
        String value = event.getMetadata("anyKey");
        
        assertThat(value).isNull();
    }

    @Test
    void shouldSupportGenericPayloadTypes() {
        // Test with Integer payload
        DomainEvent<Integer> intEvent = DomainEvent.<Integer>builder()
                .eventType("NumberEvent")
                .payload(42)
                .build();
        
        assertThat(intEvent.getPayload()).isEqualTo(42);
        
        // Test with custom object payload
        TestPayload customPayload = new TestPayload("test", 123);
        DomainEvent<TestPayload> customEvent = DomainEvent.<TestPayload>builder()
                .eventType("CustomEvent")
                .payload(customPayload)
                .build();
        
        assertThat(customEvent.getPayload()).isEqualTo(customPayload);
        assertThat(customEvent.getPayload().getName()).isEqualTo("test");
        assertThat(customEvent.getPayload().getValue()).isEqualTo(123);
    }

    @Test
    void shouldGenerateUniqueEventIds() {
        DomainEvent<String> event1 = new DomainEvent<>();
        DomainEvent<String> event2 = new DomainEvent<>();
        
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        assertThat(event1.getEventId()).isNotNull();
        assertThat(event2.getEventId()).isNotNull();
    }

    @Test
    void shouldGenerateTimestamps() {
        Instant before = Instant.now();
        DomainEvent<String> event = new DomainEvent<>();
        Instant after = Instant.now();
        
        assertThat(event.getTimestamp()).isBetween(before, after);
    }

    @Test
    void shouldSupportEquality() {
        String eventId = "same-id";
        DomainEvent<String> event1 = DomainEvent.<String>builder()
                .eventId(eventId)
                .eventType("TestEvent")
                .payload("payload")
                .build();
        
        DomainEvent<String> event2 = DomainEvent.<String>builder()
                .eventId(eventId)
                .eventType("TestEvent")
                .payload("payload")
                .build();
        
        DomainEvent<String> event3 = DomainEvent.<String>builder()
                .eventId("different-id")
                .eventType("TestEvent")
                .payload("payload")
                .build();
        
        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void shouldOverrideDefaultValues() {
        String customEventId = "custom-event-id";
        Instant customTimestamp = Instant.parse("2023-01-01T00:00:00Z");
        String customVersion = "3.0";
        
        DomainEvent<String> event = DomainEvent.<String>builder()
                .eventId(customEventId)
                .timestamp(customTimestamp)
                .version(customVersion)
                .build();
        
        assertThat(event.getEventId()).isEqualTo(customEventId);
        assertThat(event.getTimestamp()).isEqualTo(customTimestamp);
        assertThat(event.getVersion()).isEqualTo(customVersion);
    }

    // Test payload class for generic type testing
    private static class TestPayload {
        private final String name;
        private final int value;

        public TestPayload(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestPayload that = (TestPayload) obj;
            return value == that.value && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + value;
        }
    }
}