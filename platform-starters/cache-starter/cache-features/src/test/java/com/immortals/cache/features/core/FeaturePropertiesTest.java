package com.immortals.cache.features.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeaturePropertiesTest {

    @Test
    void testDefaultProperties() {
        FeatureProperties properties = new FeatureProperties();
        assertNotNull(properties.getCompression());
        assertNotNull(properties.getEncryption());
        assertNotNull(properties.getSerialization());
    }

    @Test
    void testCompressionPropertiesDefaults() {
        FeatureProperties.CompressionProperties compression = new FeatureProperties.CompressionProperties();
        assertFalse(compression.isEnabled());
        assertNull(compression.getAlgorithm());
        assertEquals(0, compression.getThreshold());
    }

    @Test
    void testCompressionPropertiesSetters() {
        FeatureProperties.CompressionProperties compression = new FeatureProperties.CompressionProperties();
        compression.setEnabled(true);
        compression.setAlgorithm("gzip");
        compression.setThreshold(1024);

        assertTrue(compression.isEnabled());
        assertEquals("gzip", compression.getAlgorithm());
        assertEquals(1024, compression.getThreshold());
    }

    @Test
    void testEncryptionPropertiesDefaults() {
        FeatureProperties.EncryptionProperties encryption = new FeatureProperties.EncryptionProperties();
        assertFalse(encryption.isEnabled());
        assertNull(encryption.getAlgorithm());
        assertNull(encryption.getKey());
        assertEquals(0, encryption.getKeySize());
    }

    @Test
    void testEncryptionPropertiesSetters() {
        FeatureProperties.EncryptionProperties encryption = new FeatureProperties.EncryptionProperties();
        encryption.setEnabled(true);
        encryption.setAlgorithm("AES-GCM");
        encryption.setKey("test-key");
        encryption.setKeySize(256);

        assertTrue(encryption.isEnabled());
        assertEquals("AES-GCM", encryption.getAlgorithm());
        assertEquals("test-key", encryption.getKey());
        assertEquals(256, encryption.getKeySize());
    }

    @Test
    void testSerializationPropertiesDefaults() {
        FeatureProperties.SerializationProperties serialization = new FeatureProperties.SerializationProperties();
        assertNull(serialization.getStrategy());
    }

    @Test
    void testSerializationPropertiesSetters() {
        FeatureProperties.SerializationProperties serialization = new FeatureProperties.SerializationProperties();
        serialization.setStrategy("jackson");

        assertEquals("jackson", serialization.getStrategy());
    }

    @Test
    void testSetCompression() {
        FeatureProperties properties = new FeatureProperties();
        FeatureProperties.CompressionProperties compression = new FeatureProperties.CompressionProperties();
        compression.setEnabled(true);

        properties.setCompression(compression);
        assertTrue(properties.getCompression()
                .isEnabled());
    }

    @Test
    void testSetEncryption() {
        FeatureProperties properties = new FeatureProperties();
        FeatureProperties.EncryptionProperties encryption = new FeatureProperties.EncryptionProperties();
        encryption.setEnabled(true);

        properties.setEncryption(encryption);
        assertTrue(properties.getEncryption()
                .isEnabled());
    }

    @Test
    void testSetSerialization() {
        FeatureProperties properties = new FeatureProperties();
        FeatureProperties.SerializationProperties serialization = new FeatureProperties.SerializationProperties();
        serialization.setStrategy("java");

        properties.setSerialization(serialization);
        assertEquals("java", properties.getSerialization()
                .getStrategy());
    }
}
