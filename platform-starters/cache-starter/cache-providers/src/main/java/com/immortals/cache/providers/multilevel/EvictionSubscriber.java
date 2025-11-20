package com.immortals.cache.providers.multilevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.immortals.cache.core.CacheService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Subscriber for distributed cache eviction events.
 * Listens to Redis Pub/Sub channels and invalidates L1 cache based on events
 * from other application instances.
 * 
 * @since 2.0.0
 */
public class EvictionSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(EvictionSubscriber.class);


    private final CacheService<Object, Object> l1Cache;
    private final ObjectMapper objectMapper;
    private final String instanceId;
    private final String namespace;

    public EvictionSubscriber(CacheService<Object, Object> l1Cache,
                             ObjectMapper objectMapper,
                             String instanceId,
                             String namespace) {
        this.l1Cache = l1Cache;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.namespace = namespace;
        log.info("Eviction subscriber initialized for namespace: {} with instance ID: {}", namespace, instanceId);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            EvictionEvent event = objectMapper.readValue(messageBody, EvictionEvent.class);
            
            // Skip events from this instance to avoid redundant evictions
            if (instanceId.equals(event.getSource())) {
                log.debug("Skipping eviction event from same instance: {} for namespace: {}", 
                        instanceId, namespace);
                return;
            }
            
            // Only process events for this namespace
            if (!namespace.equals(event.getNamespace())) {
                log.debug("Skipping eviction event for different namespace: {} (expected: {})", 
                        event.getNamespace(), namespace);
                return;
            }
            
            handleEvictionEvent(event);
        } catch (Exception e) {
            log.error("Failed to process eviction event for namespace: {}", namespace, e);
        }
    }

    private void handleEvictionEvent(EvictionEvent event) {
        log.debug("Processing L1 cache eviction event: type={}, source={}, namespace={}", 
                event.getType(), event.getSource(), event.getNamespace());
        
        switch (event.getType()) {
            case SINGLE_KEY:
                evictSingleKey(event.getKey());
                break;
            case PATTERN:
                evictByPattern(event.getPattern());
                break;
            case CLEAR_ALL:
                clearAllL1Cache();
                break;
            default:
                log.warn("Unknown eviction type: {} for namespace: {}", event.getType(), namespace);
        }
    }

    private void evictSingleKey(String key) {
        try {
            l1Cache.remove(key);
            log.debug("Evicted L1 cache key: {} in namespace: {}", key, namespace);
        } catch (Exception e) {
            log.error("Failed to evict L1 cache key: {} in namespace: {}", key, namespace, e);
        }
    }

    private void evictByPattern(String pattern) {
        try {
            // Pattern-based eviction is challenging for L1 caches without key tracking
            // For simplicity, we clear all L1 cache for pattern evictions
            log.warn("Pattern-based eviction not fully supported for L1 cache in namespace: {}, clearing all L1 entries", 
                    namespace);
            l1Cache.clear();
            log.info("Cleared L1 cache due to pattern eviction: {} in namespace: {}", pattern, namespace);
        } catch (Exception e) {
            log.error("Failed to evict L1 cache by pattern: {} in namespace: {}", pattern, namespace, e);
        }
    }

    private void clearAllL1Cache() {
        try {
            l1Cache.clear();
            log.info("Cleared all L1 cache entries for namespace: {}", namespace);
        } catch (Exception e) {
            log.error("Failed to clear all L1 cache for namespace: {}", namespace, e);
        }
    }
}
