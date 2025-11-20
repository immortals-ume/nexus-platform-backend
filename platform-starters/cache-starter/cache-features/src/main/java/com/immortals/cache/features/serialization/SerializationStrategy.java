package com.immortals.cache.features.serialization;

/**
 * Strategy interface for serialization/deserialization of cache values.
 * Implementations provide specific serialization mechanisms.
 */
public interface SerializationStrategy {
    
    /**
     * Serializes an object to bytes.
     * 
     * @param value the object to serialize
     * @return the serialized bytes
     * @throws SerializationException if serialization fails
     */
    byte[] serialize(Object value);
    
    /**
     * Deserializes bytes to an object.
     * 
     * @param data the bytes to deserialize
     * @param type the target class type
     * @param <T> the type of the object
     * @return the deserialized object
     * @throws SerializationException if deserialization fails
     */
    <T> T deserialize(byte[] data, Class<T> type);
    
    /**
     * Returns the name of the serialization format.
     * 
     * @return the format name (e.g., "JSON", "Java", "Kryo")
     */
    String getFormat();
}
