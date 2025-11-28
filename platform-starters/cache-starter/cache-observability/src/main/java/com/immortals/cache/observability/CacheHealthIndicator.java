package com.immortals.cache.observability;

import com.immortals.cache.core.CacheService;
import com.immortals.cache.core.CacheStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Health indicator for cache service.
 * Reports cache connectivity status and statistics via Spring Boot Actuator.
 * 
 * @since 2.0.0
 */

@Slf4j
public class CacheHealthIndicator implements HealthIndicator {

    private final List<CacheService<?, ?>> cacheServices;
    private final Optional<RedisConnectionFactory> redisConnectionFactory;

    public CacheHealthIndicator(
            List<CacheService<?, ?>> cacheServices,
            Optional<RedisConnectionFactory> redisConnectionFactory) {
        this.cacheServices = cacheServices;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            boolean redisHealthy = checkRedisHealth(details);

            collectCacheStatistics(details);

            if (redisHealthy || cacheServices.isEmpty()) {
                return Health.up()
                    .withDetails(details)
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "Redis connection issues detected")
                    .withDetails(details)
                    .build();
            }

        } catch (Exception e) {
            log.error("Error checking cache health", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }

    /**
     * Checks Redis connectivity.
     * 
     * @param details map to add health details to
     * @return true if Redis is healthy or not configured
     */
    private boolean checkRedisHealth(Map<String, Object> details) {
        if (redisConnectionFactory.isEmpty()) {
            details.put("redis", "not configured");
            return true;
        }

        try {
            RedisConnectionFactory factory = redisConnectionFactory.get();
            factory.getConnection().ping();

            Map<String, Object> redisDetails = new HashMap<>();
            redisDetails.put("status", "UP");
            redisDetails.put("connection", "active");
            details.put("redis", redisDetails);

            return true;
        } catch (Exception e) {
            log.warn("Redis health check failed", e);

            Map<String, Object> redisDetails = new HashMap<>();
            redisDetails.put("status", "DOWN");
            redisDetails.put("error", e.getMessage());
            details.put("redis", redisDetails);

            return false;
        }
    }

    /**
     * Collects statistics from all cache services.
     * 
     * @param details map to add statistics to
     */
    private void collectCacheStatistics(Map<String, Object> details) {
        if (cacheServices.isEmpty()) {
            details.put("caches", "none configured");
            return;
        }

        Map<String, Object> cacheStats = new HashMap<>();
        int cacheIndex = 0;

        for (CacheService<?, ?> cacheService : cacheServices) {
            try {
                CacheStatistics stats = cacheService.getStatistics();

                Map<String, Object> serviceStats = new HashMap<>();
                serviceStats.put("namespace", stats.getNamespace());
                serviceStats.put("hitRate", String.format("%.2f%%", stats.getHitRate() * 100));
                serviceStats.put("hitCount", stats.getHitCount());
                serviceStats.put("missCount", stats.getMissCount());
                serviceStats.put("evictionCount", stats.getEvictionCount());
                serviceStats.put("currentSize", stats.getCurrentSize());
                serviceStats.put("avgGetLatency", String.format("%.2fms", stats.getAvgGetLatency()));
                serviceStats.put("avgPutLatency", String.format("%.2fms", stats.getAvgPutLatency()));

                if (stats.getMemoryUsage() != null) {
                    serviceStats.put("memoryUsage", formatBytes(stats.getMemoryUsage()));
                }

                String cacheName = stats.getNamespace() != null ? stats.getNamespace() : "cache-" + cacheIndex;
                cacheStats.put(cacheName, serviceStats);
                cacheIndex++;

            } catch (Exception e) {
                log.warn("Failed to collect statistics for cache service", e);
                cacheStats.put("cache-" + cacheIndex, Map.of("error", e.getMessage()));
                cacheIndex++;
            }
        }

        details.put("caches", cacheStats);
    }

    /**
     * Formats bytes into human-readable format.
     * 
     * @param bytes the number of bytes
     * @return formatted string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
