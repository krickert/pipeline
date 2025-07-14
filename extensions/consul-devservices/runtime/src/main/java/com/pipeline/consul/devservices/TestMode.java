package com.pipeline.consul.devservices;

/**
 * Defines the different modes for running tests and development.
 * Each mode can have its own container configuration and behavior.
 * 
 * TODO: Future Extension Points
 * - Custom test modes (E2E_TEST, PERFORMANCE_TEST, etc.)
 * - Per-mode container configurations
 * - This pattern could be extracted into a general Quarkus TestContainers extension
 * - Support for mode-specific configurations in application.yml
 * - Consider contributing this pattern upstream to Quarkus
 */
public enum TestMode {
    /**
     * Production mode - no test resources loaded at all.
     * This is the default mode for normal application runtime.
     */
    PRODUCTION,
    
    /**
     * Development mode for quarkusDev.
     * Uses two-container sidecar pattern to match production architecture.
     * Enables UI, verbose logging, and seeds development data.
     */
    DEVELOPMENT,
    
    /**
     * Unit test mode for regular @Test.
     * No containers started, services should use mocks/stubs.
     * Optimized for fast test execution.
     */
    UNIT_TEST,
    
    /**
     * Integration test mode for @QuarkusIntegrationTest.
     * Uses static initialization to ensure consul-config can connect.
     * Creates isolated namespaces per test.
     */
    INTEGRATION_TEST;
    
    /**
     * Detects the current test mode based on system properties and context.
     * 
     * @return The detected TestMode
     */
    public static TestMode detect() {
        // Check explicit property first
        String explicitMode = System.getProperty("quarkus.devservices.mode");
        if (explicitMode != null) {
            try {
                return valueOf(explicitMode.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid mode specified, continue with auto-detection
            }
        }
        
        // Auto-detect based on context
        // Check if we're in integration test mode
        if ("true".equals(System.getProperty("quarkus.test.integration-test"))) {
            return INTEGRATION_TEST;
        }
        
        // Check various indicators of integration test context
        // QuarkusIntegrationTest sets test.url system property
        if (System.getProperty("test.url") != null) {
            return INTEGRATION_TEST;
        }
        
        // Check if we're running from a test runner with integration test classes
        // Look through the stack trace for integration test indicators
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (className.contains("QuarkusIntegrationTest") || 
                className.contains("IntegrationTestExtension") ||
                className.contains("IntegrationTestUtil")) {
                return INTEGRATION_TEST;
            }
        }
        
        // Check if we're in dev mode (this would be set by LaunchMode)
        if ("true".equals(System.getProperty("quarkus.launch.dev-mode"))) {
            return DEVELOPMENT;
        }
        
        // Check if we're in any test context
        // This is a bit tricky - we check for test-specific properties
        if (System.getProperty("quarkus.test") != null || 
            System.getProperty("java.class.path", "").contains("test-classes")) {
            return UNIT_TEST;
        }
        
        // Default to production
        return PRODUCTION;
    }
    
    /**
     * Whether this mode should start containers.
     */
    public boolean shouldStartContainers() {
        return this == DEVELOPMENT || this == INTEGRATION_TEST;
    }
    
    /**
     * Whether this mode should use static initialization.
     * Required for integration tests where consul-config needs early connection.
     */
    public boolean requiresStaticInit() {
        return this == INTEGRATION_TEST;
    }
}