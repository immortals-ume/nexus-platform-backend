package com.immortals.platform.messaging.publisher;

import com.immortals.platform.messaging.event.DomainEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for publishing domain events to the message broker.
 */
public interface EventPublisher {

    /**
     * Publish an event to the specified topic
     *
     * @param topic The Kafka topic to publish to
     * @param event The domain event to publish
     * @param <T> The type of the event payload
     * @return CompletableFuture that completes when the event is published
     */
    <T> CompletableFuture<Void> publish(String topic, DomainEvent<T> event);

    /**
     * Publish an event to the specified topic with a specific partition key
     *
     * @param topic The Kafka topic to publish to
     * @param key The partition key
     * @param event The domain event to publish
     * @param <T> The type of the event payload
     * @return CompletableFuture that completes when the event is published
     */
    <T> CompletableFuture<Void> publish(String topic, String key, DomainEvent<T> event);

    /**
     * Publish an event synchronously (blocks until published)
     *
     * @param topic The Kafka topic to publish to
     * @param event The domain event to publish
     * @param <T> The type of the event payload
     */
    <T> void publishSync(String topic, DomainEvent<T> event);

    /**
     * Publish an event synchronously with a specific partition key
     *
     * @param topic The Kafka topic to publish to
     * @param key The partition key
     * @param event The domain event to publish
     * @param <T> The type of the event payload
     */
    <T> void publishSync(String topic, String key, DomainEvent<T> event);
}
