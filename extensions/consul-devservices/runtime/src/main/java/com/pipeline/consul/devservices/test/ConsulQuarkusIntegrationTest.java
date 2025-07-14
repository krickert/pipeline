package com.pipeline.consul.devservices.test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation for Consul integration tests that run against packaged JARs.
 * 
 * This annotation is designed to work with @QuarkusIntegrationTest, which tests
 * the packaged application. It includes the ConsulIntegrationExtension that provides
 * test utilities for namespace isolation and cleanup.
 * 
 * For @QuarkusTest (dev mode) tests, use @ConsulIntegrationTest instead.
 * 
 * Usage:
 * <pre>
 * {@code
 * @ConsulQuarkusIntegrationTest
 * public class MyPackagedAppTest {
 *     @ConsulTest
 *     ConsulTestContext consulContext;
 *     
 *     @Test
 *     public void testSomething() {
 *         // Use consulContext for isolated testing
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@QuarkusIntegrationTest
@ExtendWith(ConsulIntegrationExtension.class)
public @interface ConsulQuarkusIntegrationTest {
    /**
     * Namespace prefix for test isolation.
     * If empty, uses test class name.
     */
    String namespacePrefix() default "";
    
    /**
     * Whether to cleanup namespace after each test method.
     */
    boolean cleanup() default true;
    
    /**
     * Timeout in seconds for Consul operations.
     */
    int timeoutSeconds() default 5;
}