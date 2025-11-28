package com.immortals.cache.providers.caffeine;


import com.immortals.platform.common.exception.CacheConfigurationException;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CaffeinePropertiesTest {

    @Test
    void testDefaultProperties() {
        CaffeineProperties properties = new CaffeineProperties();
        assertEquals(0, properties.getMaximumSize());
        assertNull(properties.getTtl());
        assertNull(properties.getEvictionPolicy());
        assertNull(properties.getRecordStats());
    }

    @Test
    void testSetMaximumSize() {
        CaffeineProperties properties = new CaffeineProperties();
        properties.setMaximumSize(1000);
        assertEquals(1000, properties.getMaximumSize());
    }

    @Test
    void testSetTtl() {
        CaffeineProperties properties = new CaffeineProperties();
        Duration ttl = Duration.ofMinutes(10);
        properties.setTtl(ttl);
        assertEquals(ttl, properties.getTtl());
    }

    @Test
    void testSetEvictionPolicy() {
        CaffeineProperties properties = new CaffeineProperties();
        properties.setEvictionPolicy(CaffeineProperties.EvictionPolicy.LRU);
        assertEquals(CaffeineProperties.EvictionPolicy.LRU, properties.getEvictionPolicy());
    }

    @Test
    void testSetRecordStats() {
        CaffeineProperties properties = new CaffeineProperties();
        properties.setRecordStats(true);
        assertTrue(properties.getRecordStats());
    }

    @Test
    void testValidateWithValidProperties() {
        CaffeineProperties properties = new CaffeineProperties();
        properties.setMaximumSize(1000);
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void testValidateWithZeroMaximumSize() {
        CaffeineProperties properties = new CaffeineProperties();
        properties.setMaximumSize(0);
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testValidateWithNegativeMaximumSize() {
        CaffeineProperties properties = new CaffeineProperties();
        properties.setMaximumSize(-1);
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testEvictionPolicyEnum() {
        assertEquals(3, CaffeineProperties.EvictionPolicy.values().length);
        assertNotNull(CaffeineProperties.EvictionPolicy.valueOf("LRU"));
        assertNotNull(CaffeineProperties.EvictionPolicy.valueOf("LFU"));
        assertNotNull(CaffeineProperties.EvictionPolicy.valueOf("SIZE"));
    }

    @Test
    void testAllEvictionPolicies() {
        CaffeineProperties properties = new CaffeineProperties();
        
        properties.setEvictionPolicy(CaffeineProperties.EvictionPolicy.LRU);
        assertEquals(CaffeineProperties.EvictionPolicy.LRU, properties.getEvictionPolicy());
        
        properties.setEvictionPolicy(CaffeineProperties.EvictionPolicy.LFU);
        assertEquals(CaffeineProperties.EvictionPolicy.LFU, properties.getEvictionPolicy());
        
        properties.setEvictionPolicy(CaffeineProperties.EvictionPolicy.SIZE);
        assertEquals(CaffeineProperties.EvictionPolicy.SIZE, properties.getEvictionPolicy());
    }
}
