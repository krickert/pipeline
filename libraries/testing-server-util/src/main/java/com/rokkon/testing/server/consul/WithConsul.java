package com.rokkon.testing.server.consul;

import io.quarkus.test.common.QuarkusTestResource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable Consul for Quarkus tests.
 * Apply this annotation to test classes that require Consul service.
 * The actual Consul container will only be started if the system property
 * "test.consul.real" is set to "true". Otherwise, Consul will be disabled
 * in the test configuration.
 */
@QuarkusTestResource(SmartConsulTestResource.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithConsul {
}
