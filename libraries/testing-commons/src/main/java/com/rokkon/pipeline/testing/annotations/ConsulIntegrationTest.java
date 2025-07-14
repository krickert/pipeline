package com.rokkon.pipeline.testing.annotations;

import com.pipeline.consul.devservices.ConsulDevServicesTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation for Consul integration tests in development mode.
 * 
 * This annotation combines @QuarkusTest with the Consul DevServices test resource,
 * ensuring that Consul is started with the sophisticated two-container sidecar pattern
 * before the test runs. This is critical for tests that use consul-config, as Consul
 * must be available during application startup.
 * 
 * This annotation is for testing in development mode (@QuarkusTest).
 * For testing packaged applications, use @ConsulQuarkusIntegrationTest instead.
 * 
 * Usage:
 * <pre>
 * {@code
 * @ConsulIntegrationTest
 * public class MyConsulTest {
 *     // Test methods here
 * }
 * }
 * </pre>
 * 
 * @see com.pipeline.consul.devservices.test.ConsulQuarkusIntegrationTest for packaged app testing
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@QuarkusTest
@QuarkusTestResource(ConsulDevServicesTestResource.class)
public @interface ConsulIntegrationTest {
}