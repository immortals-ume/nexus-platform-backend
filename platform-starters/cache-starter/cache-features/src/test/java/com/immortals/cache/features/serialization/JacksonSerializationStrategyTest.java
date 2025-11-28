package com.immortals.cache.features.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JacksonSerializationStrategyTest {

    @Test
    void testDefaultConstructor() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        assertNotNull(strategy);
        assertEquals("JSON", strategy.getFormat());
    }

    @Test
    void testCustomObjectMapperConstructor() {
        ObjectMapper customMapper = new ObjectMapper();
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy(customMapper);
        assertNotNull(strategy);
    }

    @Test
    void testSerializeString() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        String value = "test";
        byte[] serialized = strategy.serialize(value);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);
    }

    @Test
    void testSerializeNull() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        byte[] serialized = strategy.serialize(null);
        assertNotNull(serialized);
        assertEquals(0, serialized.length);
    }

    @Test
    void testSerializeMap() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);

        byte[] serialized = strategy.serialize(map);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);
    }

    @Test
    void testDeserializeString() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        String original = "test";
        byte[] serialized = strategy.serialize(original);
        String deserialized = strategy.deserialize(serialized, String.class);
        assertEquals(original, deserialized);
    }

    @Test
    void testDeserializeNull() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        String deserialized = strategy.deserialize(null, String.class);
        assertNull(deserialized);
    }

    @Test
    void testDeserializeEmptyArray() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        String deserialized = strategy.deserialize(new byte[0], String.class);
        assertNull(deserialized);
    }

    @Test
    void testSerializeDeserializeRoundTrip() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        TestObject original = new TestObject("test", 123);

        byte[] serialized = strategy.serialize(original);
        TestObject deserialized = strategy.deserialize(serialized, TestObject.class);

        assertEquals(original.name, deserialized.name);
        assertEquals(original.value, deserialized.value);
    }

    @Test
    void testSerializeInvalidObject() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        Object invalidObject = new Object() {
            @SuppressWarnings("unused")
            public Object getSelf() {
                return this;
            }
        };

        assertThrows(SerializationException.class, () -> strategy.serialize(invalidObject));
    }

    @Test
    void testDeserializeInvalidData() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        byte[] invalidData = "invalid json".getBytes();

        assertThrows(SerializationException.class,
                () -> strategy.deserialize(invalidData, TestObject.class));
    }

    @Test
    void testGetFormat() {
        JacksonSerializationStrategy strategy = new JacksonSerializationStrategy();
        assertEquals("JSON", strategy.getFormat());
    }

    static class TestObject {
        public String name;
        public int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
