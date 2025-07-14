package com.pipeline.consul.devservices.it;

import com.pipeline.consul.devservices.test.ConsulQuarkusIntegrationTest;
import com.pipeline.consul.devservices.test.ConsulTest;
import com.pipeline.consul.devservices.test.ConsulTestContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

/**
 * Integration test demonstrating usage of @ConsulQuarkusIntegrationTest.
 * This test runs against the packaged application JAR.
 */
@ConsulQuarkusIntegrationTest(namespacePrefix = "consul-it", cleanup = true)
public class ConsulQuarkusIntegrationIT {
    
    @ConsulTest
    ConsulTestContext consulContext;
    
    @Test
    public void testConsulHealthCheck() {
        // Test that the application is healthy
        RestAssured.when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
    
    @Test
    public void testConsulKVOperations() {
        // Test namespace isolation
        String key = consulContext.namespace() + "/test-key";
        String value = "test-value";
        
        // Put a value
        consulContext.consulClient()
            .putValue(key, value)
            .await().indefinitely();
        
        // Get the value back
        String retrieved = consulContext.consulClient()
            .getValue(key)
            .await().indefinitely()
            .getValue();
        
        assert value.equals(retrieved) : "Retrieved value should match";
    }
    
    @Test
    public void testServiceCreation() {
        // Demonstrate service builder
        TestService service = consulContext.createService(TestService.class)
            .withKvPrefix(consulContext.namespace() + "/service")
            .build();
        
        assert service != null : "Service should be created";
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