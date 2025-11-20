package com.immortals.platform.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generic wrapper for domain events.
 * Provides standard metadata for all events in the system.
 *
 * @param <T> The type of the event payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class DomainEvent<T> {

    /**
     * Unique identifier for this event
     */
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /**
     * Type of the event (e.g., "UserCreated", "OrderPlaced")
     */
    private String eventType;

    /**
     * ID of the aggregate that generated this event
     */
    private String aggregateId;

    /**
     * Type of the aggregate (e.g., "User", "Order")
     */
    private String aggregateType;

    /**
     * The actual event data
     */
    private T payload;

    /**
     * Timestamp when the event was created
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Correlation ID for tracing requests across services
     */
    private String correlationId;

    /**
     * Additional metadata for the event
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Version of the event schema
     */
    @Builder.Default
    private String version = "1.0";

    /**
     * Source service that published the event
     */
    private String source;

    /**
     * Add metadata to the event
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Get metadata value
     */
    public String getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
}
