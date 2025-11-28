package com.immortals.cache.providers.redis;


import com.immortals.platform.common.exception.CacheConfigurationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RedisPropertiesTest {

    @Test
    void testDefaultProperties() {
        RedisProperties properties = new RedisProperties();
        assertNull(properties.getHost());
        assertNull(properties.getPort());
        assertNotNull(properties.getCluster());
        assertNotNull(properties.getSentinel());
        assertNotNull(properties.getSsl());
        assertNotNull(properties.getAcl());
        assertNotNull(properties.getPipelining());
        assertNotNull(properties.getReadStrategy());
        assertNotNull(properties.getResilience());
    }

    @Test
    void testSetBasicProperties() {
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(6379);
        properties.setPassword("secret");
        properties.setDatabase(0);
        
        assertEquals("localhost", properties.getHost());
        assertEquals(6379, properties.getPort());
        assertEquals("secret", properties.getPassword());
        assertEquals(0, properties.getDatabase());
    }

    @Test
    void testSetTimeouts() {
        RedisProperties properties = new RedisProperties();
        Duration commandTimeout = Duration.ofSeconds(5);
        Duration ttl = Duration.ofMinutes(10);
        
        properties.setCommandTimeout(commandTimeout);
        properties.setTimeToLive(ttl);
        
        assertEquals(commandTimeout, properties.getCommandTimeout());
        assertEquals(ttl, properties.getTimeToLive());
    }

    @Test
    void testSslEnabled() {
        RedisProperties properties = new RedisProperties();
        properties.setUseSsl(true);
        assertTrue(properties.isSslEnabled());
        
        properties.setUseSsl(false);
        assertFalse(properties.isSslEnabled());
    }

    @Test
    void testAutoReconnectEnabled() {
        RedisProperties properties = new RedisProperties();
        properties.setAutoReconnect(true);
        assertTrue(properties.isAutoReconnectEnabled());
        
        properties.setAutoReconnect(false);
        assertFalse(properties.isAutoReconnectEnabled());
    }

    @Test
    void testPingBeforeActivateConnectionEnabled() {
        RedisProperties properties = new RedisProperties();
        properties.setPingBeforeActivateConnection(true);
        assertTrue(properties.isPingBeforeActivateConnectionEnabled());
        
        properties.setPingBeforeActivateConnection(false);
        assertFalse(properties.isPingBeforeActivateConnectionEnabled());
    }

    @Test
    void testClusterProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getCluster().setNodes(Arrays.asList("node1:6379", "node2:6379"));
        
        assertEquals(2, properties.getCluster().getNodes().size());
        assertTrue(properties.getCluster().getNodes().contains("node1:6379"));
    }

    @Test
    void testSentinelProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getSentinel().setMaster("mymaster");
        properties.getSentinel().setNodes(Arrays.asList("sentinel1:26379", "sentinel2:26379"));
        
        assertEquals("mymaster", properties.getSentinel().getMaster());
        assertEquals(2, properties.getSentinel().getNodes().size());
    }

    @Test
    void testSslProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getSsl().setTrustStore("/path/to/truststore");
        properties.getSsl().setTrustStorePassword("password");
        
        assertEquals("/path/to/truststore", properties.getSsl().getTrustStore());
        assertEquals("password", properties.getSsl().getTrustStorePassword());
    }

    @Test
    void testAclProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getAcl().setEnabled(true);
        properties.getAcl().setUsername("admin");
        
        assertTrue(properties.getAcl().getEnabled());
        assertEquals("admin", properties.getAcl().getUsername());
    }

    @Test
    void testPipeliningProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getPipelining().setEnabled(false);
        properties.getPipelining().setBatchSize(50);
        
        assertFalse(properties.getPipelining().getEnabled());
        assertEquals(50, properties.getPipelining().getBatchSize());
    }

    @Test
    void testReadStrategyProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getReadStrategy().setReadFromReplica(true);
        properties.getReadStrategy().setReplicaPreference("REPLICA");
        
        assertTrue(properties.getReadStrategy().getReadFromReplica());
        assertEquals("REPLICA", properties.getReadStrategy().getReplicaPreference());
    }

    @Test
    void testCircuitBreakerProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getResilience().getCircuitBreaker().setEnabled(true);
        properties.getResilience().getCircuitBreaker().setFailureRateThreshold(60);
        
        assertTrue(properties.getResilience().getCircuitBreaker().getEnabled());
        assertEquals(60, properties.getResilience().getCircuitBreaker().getFailureRateThreshold());
    }

    @Test
    void testStampedeProtectionProperties() {
        RedisProperties properties = new RedisProperties();
        properties.getResilience().getStampedeProtection().setEnabled(true);
        properties.getResilience().getStampedeProtection().setLockTimeout(Duration.ofSeconds(10));
        
        assertTrue(properties.getResilience().getStampedeProtection().getEnabled());
        assertEquals(Duration.ofSeconds(10), properties.getResilience().getStampedeProtection().getLockTimeout());
    }

    @Test
    void testValidateWithValidProperties() {
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(6379);
        
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void testValidateWithNullHost() {
        RedisProperties properties = new RedisProperties();
        properties.setPort(6379);
        
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testValidateWithEmptyHost() {
        RedisProperties properties = new RedisProperties();
        properties.setHost("  ");
        properties.setPort(6379);
        
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testValidateWithInvalidPort() {
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(0);
        
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testValidateWithNegativePort() {
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(-1);
        
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testValidateWithPortTooLarge() {
        RedisProperties properties = new RedisProperties();
        properties.setHost("localhost");
        properties.setPort(70000);
        
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testPoolProperties() {
        RedisProperties properties = new RedisProperties();
        properties.setPoolMaxTotal(100);
        properties.setPoolMaxIdle(50);
        properties.setPoolMinIdle(10);
        properties.setPoolMaxWait(Duration.ofSeconds(5));
        
        assertEquals(100, properties.getPoolMaxTotal());
        assertEquals(50, properties.getPoolMaxIdle());
        assertEquals(10, properties.getPoolMinIdle());
        assertEquals(Duration.ofSeconds(5), properties.getPoolMaxWait());
    }
}
