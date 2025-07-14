package com.rokkon.pipeline.config.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Integration tests for TransportType enum.
 * Uses the shared ObjectMapper from MapperFactory.
 */
@QuarkusIntegrationTest
public class TransportTypeIT extends TransportTypeTestBase {

    @Override
    protected ObjectMapper getObjectMapper() {
        return MapperFactory.getObjectMapper();
    }
}
