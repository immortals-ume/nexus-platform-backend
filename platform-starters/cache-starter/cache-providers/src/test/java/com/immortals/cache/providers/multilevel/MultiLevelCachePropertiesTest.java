package com.immortals.cache.providers.multilevel;

import com.immortals.platform.common.exception.CacheConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultiLevelCachePropertiesTest {

    @Test
    void testDefaultProperties() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        assertFalse(properties.isEnabled());
        assertTrue(properties.isEvictionEnabled());
        assertTrue(properties.isFallbackEnabled());
        assertTrue(properties.isLogFallbacks());
        assertNull(properties.getEvictionPublisher());
    }

    @Test
    void testSetEnabled() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEnabled(true);
        assertTrue(properties.isEnabled());
    }

    @Test
    void testSetEvictionEnabled() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEvictionEnabled(false);
        assertFalse(properties.isEvictionEnabled());
    }

    @Test
    void testSetFallbackEnabled() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setFallbackEnabled(false);
        assertFalse(properties.isFallbackEnabled());
    }

    @Test
    void testSetLogFallbacks() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setLogFallbacks(false);
        assertFalse(properties.isLogFallbacks());
    }

    @Test
    void testSetEvictionPublisher() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEvictionPublisher("redis");
        assertEquals("redis", properties.getEvictionPublisher());
    }

    @Test
    void testValidateWithValidProperties() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEvictionEnabled(true);
        properties.setEvictionPublisher("redis");
        
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void testValidateWithEvictionDisabled() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEvictionEnabled(false);
        
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void testValidateWithNullEvictionPublisher() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEvictionEnabled(true);
        properties.setEvictionPublisher(null);
        
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testValidateWithEmptyEvictionPublisher() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEvictionEnabled(true);
        properties.setEvictionPublisher("  ");
        
        assertThrows(CacheConfigurationException.class, properties::validate);
    }

    @Test
    void testAllPropertiesSet() {
        MultiLevelCacheProperties properties = new MultiLevelCacheProperties();
        properties.setEnabled(true);
        properties.setEvictionEnabled(true);
        properties.setFallbackEnabled(true);
        properties.setLogFallbacks(true);
        properties.setEvictionPublisher("redis");
        
        assertTrue(properties.isEnabled());
        assertTrue(properties.isEvictionEnabled());
        assertTrue(properties.isFallbackEnabled());
        assertTrue(properties.isLogFallbacks());
        assertEquals("redis", properties.getEvictionPublisher());
    }
}
