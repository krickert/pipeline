package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify Consul connection configuration.
 * Uses the new @ConsulIntegrationTest annotation for cleaner setup.
 */
@ConsulIntegrationTest(namespacePrefix = "connection-test")
public class ConsulConnectionIT {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    @Test
    void testConsulConfiguration() {
        // DevServices automatically configures Consul connection
        // Get configuration from system properties
        String host = System.getProperty("consul.host", "localhost");
        int port = Integer.parseInt(System.getProperty("consul.port", "8500"));
        
        assertNotNull(host);
        assertTrue(port > 0);
        assertEquals("localhost", host); // DevServices always uses localhost
    }
    
    @Test
    void testConsulConnection() {
        // Test key for this test
        String testKey = consul.namespace() + "/connection";
        String testValue = "test-value-" + System.currentTimeMillis();
        
        // Try a simple operation
        var result = consul.consulClient().putValue(testKey, testValue)
            .await().atMost(Duration.ofSeconds(5));
        
        assertTrue(result, "Put operation should succeed");
        
        // Try to read it back
        var keyValue = consul.consulClient().getValue(testKey)
            .await().atMost(Duration.ofSeconds(5));
        
        assertNotNull(keyValue, "Should be able to read the value back");
        assertEquals(testValue, keyValue.getValue(), "Value should match what we stored");
        
        // Verify agent info works
        var agentInfo = consul.consulClient().agentInfo()
            .await().atMost(Duration.ofSeconds(5));
        
        assertNotNull(agentInfo, "Should get agent info");
    }
}