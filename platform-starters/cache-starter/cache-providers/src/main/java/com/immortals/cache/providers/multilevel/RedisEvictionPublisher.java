package com.immortals.cache.providers.multilevel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

/**
 * Redis-based implementation of EvictionPublisher using Redis Pub/Sub.
 * Publishes cache eviction events to a Redis channel for distributed cache invalidation.
 * 
 * @since 2.0.0
 */
public class RedisEvictionPublisher implements EvictionPublisher {
    private static final Logger log = LoggerFactory.getLogger(RedisEvictionPublisher.class);


    private static final String EVICTION_CHANNEL_PREFIX = "cache:eviction:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    public RedisEvictionPublisher(RedisTemplate<String, Object> redisTemplate,
                                  ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = generateInstanceId();
        log.info("Redis eviction publisher initialized with instance ID: {}", instanceId);
    }

    @Override
    public void publishKeyEviction(String namespace, String key) {
        try {
            EvictionEvent event = EvictionEvent.singleKey(namespace, key, instanceId);
            publishEvent(namespace, event);
            log.debug("Published key eviction event for key: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            log.error("Failed to publish key eviction event for key: {} in namespace: {}", key, namespace, e);
        }
    }

    @Override
    public void publishPatternEviction(String namespace, String pattern) {
        try {
            EvictionEvent event = EvictionEvent.pattern(namespace, pattern, instanceId);
            publishEvent(namespace, event);
            log.debug("Published pattern eviction event for pattern: {} in namespace: {}", pattern, namespace);
        } catch (Exception e) {
            log.error("Failed to publish pattern eviction event for pattern: {} in namespace: {}",
                    pattern, namespace, e);
        }
    }

    @Override
    public void publishClearAll(String namespace) {
        try {
            EvictionEvent event = EvictionEvent.clearAll(namespace, instanceId);
            publishEvent(namespace, event);
            log.debug("Published clear all event for namespace: {}", namespace);
        } catch (Exception e) {
            log.error("Failed to publish clear all event for namespace: {}", namespace, e);
        }
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    private void publishEvent(String namespace, EvictionEvent event) {
        try {
            String channel = EVICTION_CHANNEL_PREFIX + namespace;
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, message);
            log.trace("Published eviction event to channel: {}", channel);
        } catch (Exception e) {
            log.error("Failed to serialize and publish eviction event for namespace: {}", namespace, e);
            throw new RuntimeException("Failed to publish eviction event", e);
        }
    }

    private String generateInstanceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
