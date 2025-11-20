package com.immortals.cache.core;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;

/**
 * Comprehensive cache statistics.
 * 
 * @since 2.0.0
 */
@Value
@Builder
public class CacheStatistics {

    String namespace;
    Instant timestamp;
    TimeWindow window;
    
    // Hit/Miss statistics
    long hitCount;
    long missCount;
    double hitRate;
    double missRate;
    
    // Size statistics
    long currentSize;
    long maxSize;
    double fillPercentage;
    
    // Eviction statistics
    long evictionCount;
    double evictionRate;
    
    // Latency statistics (in milliseconds)
    double avgGetLatency;
    double p50GetLatency;
    double p95GetLatency;
    double p99GetLatency;
    double maxGetLatency;
    double avgPutLatency;
    double p50PutLatency;
    double p95PutLatency;
    double p99PutLatency;
    double avgRemoveLatency;
    
    // Throughput statistics (operations per second)
    double getOpsPerSecond;
    double putOpsPerSecond;
    double removeOpsPerSecond;
    
    // Memory statistics (in bytes)
    Long memoryUsage;
    Long maxMemory;
    
    public enum TimeWindow {
        ONE_MINUTE(Duration.ofMinutes(1)),
        FIVE_MINUTES(Duration.ofMinutes(5)),
        FIFTEEN_MINUTES(Duration.ofMinutes(15)),
        ONE_HOUR(Duration.ofHours(1)),
        ALL_TIME(null);
        
        private final Duration duration;
        
        TimeWindow(Duration duration) {
            this.duration = duration;
        }
        
        public Duration getDuration() {
            return duration;
        }
    }
}
