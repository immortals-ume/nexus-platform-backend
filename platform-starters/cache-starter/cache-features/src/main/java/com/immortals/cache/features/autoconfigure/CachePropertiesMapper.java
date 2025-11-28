package com.immortals.cache.features.autoconfigure;

import com.immortals.cache.providers.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Utility class for mapping CacheProperties to provider-specific properties.
 *
 * <p>Centralizes property mapping logic to reduce duplication across
 * autoconfiguration classes and ensure consistency.
 *
 * @since 2.0.0
 */
@Slf4j
public class CachePropertiesMapper {

    private CachePropertiesMapper() {
    }

    /**
     * Maps CacheProperties Redis configuration to RedisProperties (from cache-providers).
     *
     * @param cacheProperties main cache properties
     * @return mapped Redis properties with all defaults applied
     * @throws IllegalArgumentException if required properties are missing
     */
    public static RedisProperties mapToRedisProperties(CacheProperties cacheProperties) {
        if (cacheProperties == null) {
            throw new IllegalArgumentException("CacheProperties cannot be null");
        }

        RedisProperties redisConfig = cacheProperties.getRedisProperties();
        if (redisConfig == null) {
            throw new IllegalArgumentException("Redis configuration is missing in CacheProperties");
        }

        RedisProperties properties = new RedisProperties();

        properties.setHost(redisConfig.getHost() != null ? redisConfig.getHost() : "localhost");
        properties.setPort(redisConfig.getPort() != null ? redisConfig.getPort() : 6379);
        properties.setPassword(redisConfig.getPassword());
        properties.setDatabase(redisConfig.getDatabase() != null ? redisConfig.getDatabase() : 0);
        properties.setCommandTimeout(redisConfig.getCommandTimeout() != null ? redisConfig.getCommandTimeout() : Duration.ofSeconds(5));
        properties.setUseSsl(redisConfig.getUseSsl() != null ? redisConfig.getUseSsl() : false);
        properties.setAutoReconnect(redisConfig.getAutoReconnect() != null ? redisConfig.getAutoReconnect() : true);
        properties.setPingBeforeActivateConnection(redisConfig.getPingBeforeActivateConnection() != null ? redisConfig.getPingBeforeActivateConnection() : true);
        properties.setTimeToLive(cacheProperties.getDefaultTtl());
        properties.setEnable(redisConfig.getEnable() != null ? redisConfig.getEnable() : true);

        properties.setPoolMaxTotal(redisConfig.getPoolMaxTotal() != null ? redisConfig.getPoolMaxTotal() : 8);
        properties.setPoolMaxIdle(redisConfig.getPoolMaxIdle() != null ? redisConfig.getPoolMaxIdle() : 8);
        properties.setPoolMinIdle(redisConfig.getPoolMinIdle() != null ? redisConfig.getPoolMinIdle() : 2);
        properties.setPoolMaxWait(redisConfig.getPoolMaxWait() != null ? redisConfig.getPoolMaxWait() : Duration.ofMillis(-1));

        if (redisConfig.getSsl() != null) {
            RedisProperties.Ssl ssl = new RedisProperties.Ssl();
            ssl.setTrustStore(redisConfig.getSsl()
                    .getTrustStore());
            ssl.setTrustStorePassword(redisConfig.getSsl()
                    .getTrustStorePassword());
            ssl.setKeyStore(redisConfig.getSsl()
                    .getKeyStore());
            ssl.setKeyStorePassword(redisConfig.getSsl()
                    .getKeyStorePassword());
            properties.setSsl(ssl);
        }

        if (redisConfig.getAcl() != null) {
            RedisProperties.Acl acl = new RedisProperties.Acl();
            acl.setEnabled(redisConfig.getAcl()
                    .getEnabled() != null ? redisConfig.getAcl()
                    .getEnabled() : false);
            acl.setUsername(redisConfig.getAcl()
                    .getUsername());
            properties.setAcl(acl);
        }

        if (redisConfig.getPipelining() != null) {
            RedisProperties.Pipelining pipelining = new RedisProperties.Pipelining();
            pipelining.setEnabled(redisConfig.getPipelining()
                    .getEnabled() != null ? redisConfig.getPipelining()
                    .getEnabled() : true);
            pipelining.setBatchSize(redisConfig.getPipelining()
                    .getBatchSize() != null ? redisConfig.getPipelining()
                    .getBatchSize() : 100);
            properties.setPipelining(pipelining);
        }

        if (redisConfig.getCluster() != null && !redisConfig.getCluster()
                .getNodes()
                .isEmpty()) {
            RedisProperties.Cluster cluster = new RedisProperties.Cluster();
            cluster.setNodes(redisConfig.getCluster()
                    .getNodes());
            properties.setCluster(cluster);
        }

        if (redisConfig.getSentinel() != null && redisConfig.getSentinel()
                .getMaster() != null) {
            RedisProperties.Sentinel sentinel = new RedisProperties.Sentinel();
            sentinel.setMaster(redisConfig.getSentinel()
                    .getMaster());
            sentinel.setNodes(redisConfig.getSentinel()
                    .getNodes());
            properties.setSentinel(sentinel);
        }

        if (redisConfig.getReadStrategy() != null) {
            RedisProperties.ReadStrategy readStrategy = new RedisProperties.ReadStrategy();
            readStrategy.setReadFromReplica(redisConfig.getReadStrategy()
                    .getReadFromReplica() != null ? redisConfig.getReadStrategy()
                    .getReadFromReplica() : false);
            readStrategy.setReplicaPreference(redisConfig.getReadStrategy()
                    .getReplicaPreference() != null ? redisConfig.getReadStrategy()
                    .getReplicaPreference() : "REPLICA_PREFERRED");
            properties.setReadStrategy(readStrategy);
        }

        log.debug("Mapped Redis properties: host={}, port={}, database={}, useSsl={}, autoReconnect={}",
                properties.getHost(), properties.getPort(), properties.getDatabase(), properties.getUseSsl(), properties.getAutoReconnect());

        return properties;
    }

    /**
     * Validates Redis configuration for consistency.
     *
     * @param cacheProperties cache properties to validate
     * @throws IllegalStateException if the configuration is invalid
     */
    public static void validateRedisConfiguration(CacheProperties cacheProperties) {
        RedisProperties redisConfig = cacheProperties.getRedisProperties();
        if (redisConfig == null) {
            return;
        }

        boolean hasCluster = redisConfig.getCluster() != null && !redisConfig.getCluster()
                .getNodes()
                .isEmpty();
        boolean hasSentinel = redisConfig.getSentinel() != null && redisConfig.getSentinel()
                .getMaster() != null;

        if (hasCluster && hasSentinel) {
            throw new IllegalStateException("Cannot configure both cluster and sentinel modes simultaneously");
        }

        if (Boolean.TRUE.equals(redisConfig.getUseSsl()) && redisConfig.getSsl() != null) {
            if (redisConfig.getSsl()
                    .getTrustStore() != null) {
                java.io.File trustStoreFile = new java.io.File(redisConfig.getSsl()
                        .getTrustStore());
                if (!trustStoreFile.exists()) {
                    throw new IllegalStateException("SSL trust store file not found: " + redisConfig.getSsl()
                            .getTrustStore());
                }
            }

            if (redisConfig.getSsl()
                    .getKeyStore() != null) {
                java.io.File keyStoreFile = new java.io.File(redisConfig.getSsl()
                        .getKeyStore());
                if (!keyStoreFile.exists()) {
                    throw new IllegalStateException("SSL key store file not found: " + redisConfig.getSsl()
                            .getKeyStore());
                }
            }
        }

        if (redisConfig.getAcl() != null && Boolean.TRUE.equals(redisConfig.getAcl()
                .getEnabled())) {
            if (redisConfig.getAcl()
                    .getUsername() == null || redisConfig.getAcl()
                    .getUsername()
                    .trim()
                    .isEmpty()) {
                throw new IllegalStateException("ACL is enabled but username is not provided (immortals.cache.redis-properties.acl.username)");
            }
        }

        log.debug("Redis configuration validation passed");
    }
}
