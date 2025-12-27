package com.immortals.platform.messaging.publisher;

import com.immortals.platform.messaging.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test class for EventPublisher interface contract.
 */
class EventPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    private DomainEvent<String> testEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testEvent = new DomainEvent<>("test-event-id", "TestEvent", "test-payload", Instant.now());
    }

    @Test
    void shouldPublishEventAsynchronously() {
        String topic = "test-topic";
        CompletableFuture<Void> expectedFuture = CompletableFuture.completedFuture(null);
        
        when(eventPublisher.publish(topic, testEvent)).thenReturn(expectedFuture);
        
        CompletableFuture<Void> result = eventPublisher.publish(topic, testEvent);
        
        assertThat(result).isEqualTo(expectedFuture);
        verify(eventPublisher).publish(topic, testEvent);
    }

    @Test
    void shouldPublishEventWithKeyAsynchronously() {
        String topic = "test-topic";
        String key = "test-key";
        CompletableFuture<Void> expectedFuture = CompletableFuture.completedFuture(null);
        
        when(eventPublisher.publish(topic, key, testEvent)).thenReturn(expectedFuture);
        
        CompletableFuture<Void> result = eventPublisher.publish(topic, key, testEvent);
        
        assertThat(result).isEqualTo(expectedFuture);
        verify(eventPublisher).publish(topic, key, testEvent);
    }

    @Test
    void shouldPublishEventSynchronously() {
        String topic = "test-topic";
        
        eventPublisher.publishSync(topic, testEvent);
        
        verify(eventPublisher).publishSync(topic, testEvent);
    }

    @Test
    void shouldPublishEventWithKeySynchronously() {
        String topic = "test-topic";
        String key = "test-key";
        
        eventPublisher.publishSync(topic, key, testEvent);
        
        verify(eventPublisher).publishSync(topic, key, testEvent);
    }

    @Test
    void shouldHandleNullTopic() {
        String nullTopic = null;
        
        eventPublisher.publish(nullTopic, testEvent);
        
        verify(eventPublisher).publish(nullTopic, testEvent);
    }

    @Test
    void shouldHandleNullKey() {
        String topic = "test-topic";
        String nullKey = null;
        
        eventPublisher.publish(topic, nullKey, testEvent);
        
        verify(eventPublisher).publish(topic, nullKey, testEvent);
    }

    @Test
    void shouldHandleNullEvent() {
        String topic = "test-topic";
        DomainEvent<String> nullEvent = null;
        
        eventPublisher.publish(topic, nullEvent);
        
        verify(eventPublisher).publish(topic, nullEvent);
    }

    @Test
    void shouldSupportGenericEventTypes() {
        DomainEvent<Integer> integerEvent = new DomainEvent<>("int-event-id", "IntegerEvent", 42, Instant.now());
        String topic = "integer-topic";
        CompletableFuture<Void> expectedFuture = CompletableFuture.completedFuture(null);
        
        when(eventPublisher.publish(topic, integerEvent)).thenReturn(expectedFuture);
        
        CompletableFuture<Void> result = eventPublisher.publish(topic, integerEvent);
        
        assertThat(result).isEqualTo(expectedFuture);
        verify(eventPublisher).publish(topic, integerEvent);
    }

    @Test
    void shouldSupportComplexEventPayloads() {
        TestPayload payload = new TestPayload("test-name", 123);
        DomainEvent<TestPayload> complexEvent = new DomainEvent<>("complex-event-id", "ComplexEvent", payload, Instant.now());
        String topic = "complex-topic";
        
        eventPublisher.publishSync(topic, complexEvent);
        
        verify(eventPublisher).publishSync(topic, complexEvent);
    }

    @Test
    void shouldAllowMultiplePublishCalls() {
        String topic1 = "topic-1";
        String topic2 = "topic-2";
        DomainEvent<String> event1 = new DomainEvent<>("event-1", "Event1", "payload-1", Instant.now());
        DomainEvent<String> event2 = new DomainEvent<>("event-2", "Event2", "payload-2", Instant.now());
        
        eventPublisher.publishSync(topic1, event1);
        eventPublisher.publishSync(topic2, event2);
        
        verify(eventPublisher).publishSync(topic1, event1);
        verify(eventPublisher).publishSync(topic2, event2);
    }

    // Test payload class for complex event testing
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
    }
}