package com.rokkon.pipeline.consul.test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation for Consul integration tests.
 * Combines @QuarkusIntegrationTest with our custom Consul test extension.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@QuarkusIntegrationTest
@ExtendWith(ConsulIntegrationExtension.class)
public @interface ConsulIntegrationTest {
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