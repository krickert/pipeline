/**
 * Test support utilities for Consul integration tests.
 * 
 * <h2>Annotation Types</h2>
 * 
 * <h3>@ConsulQuarkusIntegrationTest</h3>
 * <p>Use this annotation for testing packaged applications with @QuarkusIntegrationTest.
 * This annotation provides:</p>
 * <ul>
 *   <li>Integration with packaged JAR testing</li>
 *   <li>Namespace isolation via ConsulTestContext</li>
 *   <li>Automatic cleanup after tests</li>
 *   <li>Test utility injection via @ConsulTest fields</li>
 * </ul>
 * 
 * <pre>{@code
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
 * }</pre>
 * 
 * <h3>@ConsulIntegrationTest (in testing-commons)</h3>
 * <p>Use this annotation for testing in development mode with @QuarkusTest.
 * This annotation is available in the testing-commons library and provides
 * the same DevServices functionality but for development mode testing.</p>
 * 
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link ConsulTestContext} - Provides test utilities and namespace isolation</li>
 *   <li>{@link ConsulTest} - Field annotation for injecting ConsulTestContext</li>
 *   <li>{@link ConsulIntegrationExtension} - JUnit extension that powers the functionality</li>
 *   <li>{@link SimpleConsulClientFactory} - Creates ConsulClient instances for tests</li>
 * </ul>
 * 
 * <h2>Usage in Engine Integration Tests</h2>
 * <p>The @ConsulQuarkusIntegrationTest annotation is designed to be heavily used
 * in engine integration tests where testing against packaged applications is critical
 * for ensuring production-like behavior.</p>
 */
package com.pipeline.consul.devservices.test;