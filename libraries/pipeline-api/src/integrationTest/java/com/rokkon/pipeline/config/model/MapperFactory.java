package com.rokkon.pipeline.config.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Factory for creating and providing a singleton ObjectMapper instance for integration tests.
 * This avoids creating multiple ObjectMapper instances across test classes.
 */
public class MapperFactory {
    
    private static final ObjectMapper INSTANCE;
    
    static {
        // Use the builder pattern instead of the deprecated configure method
        INSTANCE = JsonMapper.builder()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
    }
    
    /**
     * Returns the singleton ObjectMapper instance.
     * 
     * @return the configured ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return INSTANCE;
    }
    
    // Private constructor to prevent instantiation
    private MapperFactory() {
    }
}