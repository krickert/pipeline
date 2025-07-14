package com.rokkon.pipeline.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for creating ObjectMapper instances with consistent configuration.
 * This is primarily for use in tests and standalone applications that don't have
 * access to Quarkus CDI injection.
 * <p>
 * In Quarkus applications, use @Inject ObjectMapper instead to get the
 * properly configured instance.
 */
public class ObjectMapperFactory {

    /**
     * Default constructor for ObjectMapperFactory.
     * This class only provides static methods, so the constructor is not used.
     */
    public ObjectMapperFactory() {
        // No initialization needed
    }

    /**
     * Creates an ObjectMapper with the same configuration as JsonOrderingCustomizer.
     * This ensures consistent JSON serialization across the entire application.
     * <p>
     * Configuration includes:
     * - Properties sorted alphabetically
     * - Map entries sorted by keys
     * - Property names in snake_case (data scientist friendly!)
     * - Dates written as ISO-8601 strings (not timestamps)
     * - Durations written as ISO-8601 strings (not timestamps)
     *
     * @return A configured ObjectMapper instance
     */
    public static ObjectMapper createConfiguredMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
                .build();
    }

    /**
     * Creates an ObjectMapper with minimal configuration for testing edge cases.
     * This does NOT include the ordering configuration.
     *
     * @return A minimally configured ObjectMapper instance
     */
    public static ObjectMapper createMinimalMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
                .build();
    }
}