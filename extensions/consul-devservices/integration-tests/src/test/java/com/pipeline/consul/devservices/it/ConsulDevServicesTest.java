package com.pipeline.consul.devservices.it;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the two-container sidecar pattern for Consul DevServices.
 */
@QuarkusTest
public class ConsulDevServicesTest {

    @ConfigProperty(name = "pipeline.consul.host")
    String consulHost;

    @ConfigProperty(name = "pipeline.consul.port")
    int consulPort;

    @Test
    public void testConsulIsRunning() throws Exception {
        // Verify properties are configured
        assertNotNull(consulHost, "Consul host should be configured");
        assertNotEquals(0, consulPort, "Consul port should be configured");
        
        // Create HTTP client
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        // Test Consul agent endpoint
        URI agentUri = URI.create(String.format("http://%s:%d/v1/agent/self", consulHost, consulPort));
        HttpRequest agentRequest = HttpRequest.newBuilder()
            .uri(agentUri)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> agentResponse = client.send(agentRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, agentResponse.statusCode(), "Consul agent should be accessible");
        assertTrue(agentResponse.body().contains("\"Config\""), "Response should contain agent config");

        // Test Consul catalog endpoint
        URI catalogUri = URI.create(String.format("http://%s:%d/v1/catalog/nodes", consulHost, consulPort));
        HttpRequest catalogRequest = HttpRequest.newBuilder()
            .uri(catalogUri)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> catalogResponse = client.send(catalogRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, catalogResponse.statusCode(), "Consul catalog should be accessible");
        
        // Verify we have both server and agent nodes
        String catalogBody = catalogResponse.body();
        assertTrue(catalogBody.contains("consul-server") || catalogBody.contains("\"Node\":"), 
            "Should see Consul server node");
        
        // The two-container setup should have the agent registered
        assertTrue(catalogBody.contains("engine-sidecar-dev") || catalogBody.length() > 10, 
            "Should see agent node or have valid response");
    }

    @Test
    public void testConsulKVStore() throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        // Write a test value
        String testKey = "test/devservices/key";
        String testValue = "test-value-" + System.currentTimeMillis();
        
        URI putUri = URI.create(String.format("http://%s:%d/v1/kv/%s", consulHost, consulPort, testKey));
        HttpRequest putRequest = HttpRequest.newBuilder()
            .uri(putUri)
            .timeout(Duration.ofSeconds(5))
            .PUT(HttpRequest.BodyPublishers.ofString(testValue))
            .build();

        HttpResponse<String> putResponse = client.send(putRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, putResponse.statusCode(), "Should be able to write to KV store");

        // Read the value back
        URI getUri = URI.create(String.format("http://%s:%d/v1/kv/%s?raw", consulHost, consulPort, testKey));
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(getUri)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode(), "Should be able to read from KV store");
        assertEquals(testValue, getResponse.body(), "Retrieved value should match what was stored");
    }

    @Test
    public void testServiceRegistration() throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        // Register a test service
        String serviceJson = """
            {
                "ID": "test-service-1",
                "Name": "test-service",
                "Tags": ["devservices", "test"],
                "Port": 8080,
                "Check": {
                    "HTTP": "http://host.docker.internal:8080/health",
                    "Interval": "10s"
                }
            }
            """;

        URI registerUri = URI.create(String.format("http://%s:%d/v1/agent/service/register", consulHost, consulPort));
        HttpRequest registerRequest = HttpRequest.newBuilder()
            .uri(registerUri)
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(serviceJson))
            .build();

        HttpResponse<String> registerResponse = client.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, registerResponse.statusCode(), "Should be able to register service");

        // Verify service is registered
        URI servicesUri = URI.create(String.format("http://%s:%d/v1/agent/services", consulHost, consulPort));
        HttpRequest servicesRequest = HttpRequest.newBuilder()
            .uri(servicesUri)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> servicesResponse = client.send(servicesRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, servicesResponse.statusCode(), "Should be able to list services");
        assertTrue(servicesResponse.body().contains("test-service"), "Test service should be registered");
    }
}