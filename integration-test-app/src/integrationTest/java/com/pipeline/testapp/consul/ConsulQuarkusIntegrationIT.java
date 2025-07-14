package com.pipeline.testapp.consul;

import com.pipeline.consul.devservices.test.ConsulQuarkusIntegrationTest;
import com.pipeline.consul.devservices.test.ConsulTest;
import com.pipeline.consul.devservices.test.ConsulTestContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating usage of @ConsulQuarkusIntegrationTest.
 * This test runs against the packaged application JAR.
 * 
 * Note: @QuarkusIntegrationTest does not support CDI injection,
 * but the @ConsulTest field injection works via the JUnit extension.
 */
@ConsulQuarkusIntegrationTest(namespacePrefix = "test-app", cleanup = true)
public class ConsulQuarkusIntegrationIT {
    
    @ConsulTest
    ConsulTestContext consulContext;
    
    @Test
    public void testApplicationHealthWithConsul() {
        // Test that the application is healthy when Consul is available
        RestAssured.when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
    
    @Test
    public void testConsulConnectivity() {
        // Verify we can connect to Consul through the test context
        assertNotNull(consulContext, "ConsulTestContext should be injected");
        assertNotNull(consulContext.consulClient(), "ConsulClient should be available");
        
        // Test basic connectivity
        var agentInfo = consulContext.consulClient()
            .agentInfo()
            .await().indefinitely();
        
        assertNotNull(agentInfo, "Should get agent info from Consul");
    }
    
    @Test
    public void testConsulKVOperations() {
        // Test namespace isolation
        String key = consulContext.namespace() + "/test-key";
        String value = "test-value-" + System.currentTimeMillis();
        
        // Put a value
        Boolean putResult = consulContext.consulClient()
            .putValue(key, value)
            .await().indefinitely();
        
        assertTrue(putResult, "Put operation should succeed");
        
        // Get the value back
        var keyValue = consulContext.consulClient()
            .getValue(key)
            .await().indefinitely();
        
        assertNotNull(keyValue, "Should retrieve stored value");
        assertEquals(value, keyValue.getValue(), "Retrieved value should match");
    }
    
    @Test
    public void testNamespaceIsolation() {
        // Each test method should have its own namespace
        assertNotNull(consulContext.namespace(), "Namespace should be set");
        assertTrue(consulContext.namespace().startsWith("test-app"), 
            "Namespace should start with configured prefix");
        assertTrue(consulContext.namespace().contains("testNamespaceIsolation"), 
            "Namespace should contain test method name");
    }
    
    @Test
    public void testServiceBuilder() {
        // Demonstrate service builder without CDI
        TestService service = consulContext.createService(TestService.class)
            .withKvPrefix(consulContext.namespace() + "/service")
            .build();
        
        assertNotNull(service, "Service should be created");
        assertNotNull(service.getKvPrefix(), "KV prefix should be injected");
        assertEquals(consulContext.namespace() + "/service", service.getKvPrefix());
        assertNotNull(service.consulClient, "ConsulClient should be injected");
    }
    
    // Simple test service for demonstration
    public static class TestService {
        private io.vertx.mutiny.ext.consul.ConsulClient consulClient;
        private String kvPrefix;
        
        public String getKvPrefix() {
            return kvPrefix;
        }
    }
}