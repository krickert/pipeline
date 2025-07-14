package com.rokkon.pipeline.testing.annotations;

/**
 * Placeholder for documentation purposes.
 * 
 * For testing packaged applications with Consul, use:
 * {@code @com.pipeline.consul.devservices.test.ConsulQuarkusIntegrationTest}
 * 
 * For testing in development mode with Consul, use:
 * {@code @ConsulIntegrationTest}
 * 
 * The @ConsulQuarkusIntegrationTest annotation provides:
 * - Integration with @QuarkusIntegrationTest for packaged JAR testing
 * - Namespace isolation for test data
 * - Automatic cleanup after tests
 * - Test utility injection via @ConsulTest fields
 * 
 * @deprecated This is not an actual annotation - use com.pipeline.consul.devservices.test.ConsulQuarkusIntegrationTest
 */
@Deprecated
public class ConsulQuarkusIntegrationTest {
    private ConsulQuarkusIntegrationTest() {
        throw new UnsupportedOperationException(
            "Use @com.pipeline.consul.devservices.test.ConsulQuarkusIntegrationTest annotation instead");
    }
}