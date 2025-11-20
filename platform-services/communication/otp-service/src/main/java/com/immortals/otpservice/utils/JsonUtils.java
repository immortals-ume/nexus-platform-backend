package com.immortals.otpservice.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.immortals.authapp.service.exception.AuthException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/**
 * Utility class for JSON serialization and deserialization using Jackson.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = createDefaultMapper();
    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, Boolean.FALSE);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, Boolean.FALSE);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Returns the singleton ObjectMapper instance.
     */
    public static ObjectMapper getMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param obj the object to serialize
     * @return the JSON string
     * @throws AuthException if serialization fails
     */
    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new AuthException("JSON serialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes a JSON string to an object of the given class.
     *
     * @param json  the JSON string
     * @param clazz the target class
     * @param <T>   the type of the returned object
     * @return the deserialized object
     * @throws AuthException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new AuthException("JSON deserialization failed for class " + clazz.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes a JSON string to an object using a TypeReference (for generic types).
     *
     * @param json    the JSON string
     * @param typeRef the type reference
     * @param <T>     the type of the returned object
     * @return the deserialized object
     * @throws AuthException if deserialization fails
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (IOException e) {
            throw new AuthException("JSON deserialization failed for type " + typeRef.getType() + ": " + e.getMessage(), e);
        }
    }
}
