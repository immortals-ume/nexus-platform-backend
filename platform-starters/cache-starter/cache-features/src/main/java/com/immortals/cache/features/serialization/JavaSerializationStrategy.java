package com.immortals.cache.features.serialization;

import java.io.*;

/**
 * Java native serialization strategy.
 * Uses Java's built-in serialization mechanism.
 * 
 * Advantages:
 * - No external dependencies
 * - Handles complex object graphs
 * - Preserves object identity
 * 
 * Disadvantages:
 * - Not language-agnostic
 * - Larger payload size
 * - Slower than binary formats
 * - Requires Serializable interface
 */
public class JavaSerializationStrategy implements SerializationStrategy {
    
    @Override
    public byte[] serialize(Object value) {
        if (value == null) {
            return new byte[0];
        }
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize value using Java serialization", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> type) {
        if (data == null || data.length == 0) {
            return null;
        }
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object obj = ois.readObject();
            return type.cast(obj);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Failed to deserialize value using Java serialization", e);
        }
    }
    
    @Override
    public String getFormat() {
        return "Java";
    }
}
