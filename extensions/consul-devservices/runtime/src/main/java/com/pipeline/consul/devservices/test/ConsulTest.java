package com.pipeline.consul.devservices.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for injection of ConsulTestContext.
 * Used with @ConsulQuarkusIntegrationTest to inject test utilities.
 * 
 * Usage:
 * <pre>
 * {@code
 * @ConsulQuarkusIntegrationTest
 * public class MyTest {
 *     @ConsulTest
 *     ConsulTestContext consulContext;
 * }
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsulTest {
}