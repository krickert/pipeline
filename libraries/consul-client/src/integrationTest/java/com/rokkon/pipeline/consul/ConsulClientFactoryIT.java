package com.rokkon.pipeline.consul;

import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.mutiny.ext.consul.ConsulClient;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.ext.consul.ServiceList;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ConsulClientFactory.
 * Tests that the factory can create a working Consul client and connect to Consul.
 */
@ConsulIntegrationTest(namespacePrefix = "client-factory-test")
class ConsulClientFactoryIT {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    @Test
    void testConsulClientCreation() {
        // Get the ConsulClient from the test context
        ConsulClient client = consul.consulClient();
        
        assertNotNull(client, "ConsulClient should not be null");
        
        // Test that the client can connect to Consul
        JsonObject agentInfo = client.agentInfo()
            .await().atMost(Duration.ofSeconds(5));
        
        assertNotNull(agentInfo, "Agent info should not be null");
        assertTrue(agentInfo.containsKey("Config"), "Agent info should contain Config");
        
        JsonObject config = agentInfo.getJsonObject("Config");
        assertNotNull(config, "Config should not be null");
        assertTrue(config.containsKey("Version"), "Config should contain Version");
        
        String version = config.getString("Version");
        assertNotNull(version, "Version should not be null");
        assertFalse(version.isEmpty(), "Version should not be empty");
    }
    
    @Test
    void testConsulClientKVOperations() {
        ConsulClient client = consul.consulClient();
        
        // Test namespace
        String namespace = consul.namespace();
        assertNotNull(namespace, "Namespace should not be null");
        assertTrue(namespace.startsWith("client-factory-test-"), "Namespace should start with prefix");
        
        // Test KV operations
        String key = namespace + "/test-key";
        String value = "test-value";
        
        // Put value
        Boolean putResult = client.putValue(key, value)
            .await().atMost(Duration.ofSeconds(5));
        assertTrue(putResult, "Put value should succeed");
        
        // Get value
        var keyValue = client.getValue(key)
            .await().atMost(Duration.ofSeconds(5));
        assertNotNull(keyValue, "KeyValue should not be null");
        assertEquals(value, keyValue.getValue(), "Retrieved value should match");
        
        // Delete value
        client.deleteValue(key)
            .await().atMost(Duration.ofSeconds(5));
        
        // Verify deletion
        var deletedValue = client.getValue(key)
            .await().atMost(Duration.ofSeconds(5));
        // Consul returns an empty KeyValue object instead of null for deleted keys
        assertTrue(deletedValue == null || deletedValue.getValue() == null, 
            "Value should be deleted");
    }
    
    @Test
    void testConsulClientServiceRegistration() {
        ConsulClient client = consul.consulClient();
        
        // Create a test service
        String serviceId = "test-service-" + System.currentTimeMillis();
        String serviceName = "test-service";
        
        ServiceOptions service = new ServiceOptions()
            .setId(serviceId)
            .setName(serviceName)
            .setTags(List.of("test"))
            .setAddress("localhost")
            .setPort(8080);
        
        // Register service
        client.registerService(service)
            .await().atMost(Duration.ofSeconds(5));
        
        // Verify service is registered
        ServiceList services = client.catalogServiceNodes(serviceName)
            .await().atMost(Duration.ofSeconds(5));
        assertNotNull(services, "Services should not be null");
        assertNotNull(services.getList(), "Service list should not be null");
        assertFalse(services.getList().isEmpty(), "Services should not be empty");
        
        boolean found = services.getList().stream()
            .anyMatch(s -> serviceId.equals(s.getId()));
        assertTrue(found, "Service should be found in catalog");
        
        // Deregister service
        client.deregisterService(serviceId)
            .await().atMost(Duration.ofSeconds(5));
        
        // Verify service is deregistered
        services = client.catalogServiceNodes(serviceName)
            .await().atMost(Duration.ofSeconds(5));
        if (services != null && services.getList() != null) {
            found = services.getList().stream()
                .anyMatch(s -> serviceId.equals(s.getId()));
            assertFalse(found, "Service should not be found after deregistration");
        }
    }
    
    @Test
    void testConsulClientHealthCheck() {
        ConsulClient client = consul.consulClient();
        
        // Test Consul health endpoint
        var leaderStatus = client.leaderStatus()
            .await().atMost(Duration.ofSeconds(5));
        
        assertNotNull(leaderStatus, "Leader status should not be null");
        assertFalse(leaderStatus.isEmpty(), "Leader status should not be empty");
        
        // Test agent self info
        var selfInfo = client.agentInfo()
            .await().atMost(Duration.ofSeconds(5));
        
        assertNotNull(selfInfo, "Self info should not be null");
        assertTrue(selfInfo.containsKey("Member"), "Self info should contain Member");
        
        JsonObject member = selfInfo.getJsonObject("Member");
        assertNotNull(member, "Member should not be null");
        assertTrue(member.containsKey("Name"), "Member should contain Name");
        assertTrue(member.containsKey("Status"), "Member should contain Status");
        assertEquals(1, member.getInteger("Status"), "Member status should be 1 (alive)");
    }
}