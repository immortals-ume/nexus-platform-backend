package com.immortals.cache.features.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON serialization strategy using Jackson.
 * This is the default serialization strategy for the cache service.
 * 
 * Advantages:
 * - Human-readable format
 * - Language-agnostic
 * - Good for debugging
 * - Handles Java 8 time types
 */
public class JacksonSerializationStrategy implements SerializationStrategy {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a Jackson serialization strategy with default configuration.
     */
    public JacksonSerializationStrategy() {
        this(createDefaultObjectMapper());
    }
    
    /**
     * Creates a Jackson serialization strategy with a custom ObjectMapper.
     * 
     * @param objectMapper the ObjectMapper to use
     */
    public JacksonSerializationStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
    @Override
    public byte[] serialize(Object value) {
        if (value == null) {
            return new byte[0];
        }
        
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize value using Jackson", e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> type) {
        if (data == null || data.length == 0) {
            return null;
        }
        
        try {
            return objectMapper.readValue(data, type);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize value using Jackson", e);
        }
    }
    
    @Override
    public String getFormat() {
        return "JSON";
    }
}
