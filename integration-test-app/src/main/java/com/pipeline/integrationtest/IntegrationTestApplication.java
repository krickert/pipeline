package com.pipeline.integrationtest;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Main application class for the integration test environment.
 * This provides a minimal Quarkus application for testing extensions
 * and libraries without requiring the full engine infrastructure.
 */
@QuarkusMain
public class IntegrationTestApplication {
    
    public static void main(String... args) {
        Quarkus.run(args);
    }
}