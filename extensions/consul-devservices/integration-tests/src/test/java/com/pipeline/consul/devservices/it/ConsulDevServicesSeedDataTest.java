package com.pipeline.consul.devservices.it;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.WithTestResource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies Consul DevServices seed data functionality.
 */
@QuarkusTest
@WithTestResource(ConsulStaticTestResourceWithSeedData.class)
public class ConsulDevServicesSeedDataTest {

    @ConfigProperty(name = "pipeline.consul.host")
    String consulHost;

    @ConfigProperty(name = "pipeline.consul.port")
    int consulPort;

    @Test
    public void testSeedDataIsLoaded() throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        // Check that seeded configuration values are present
        String[] testKeys = {
            "config/application/pipeline.engine.name",
            "config/application/pipeline.engine.version",
            "config/test/quarkus.http.port",
            "config/test/quarkus.grpc.server.port",
            "config/test/pipeline.engine.test-mode"
        };

        String[] expectedValues = {
            "pipeline-engine-test",
            "1.0.0-TEST",
            "39001",
            "49001",
            "true"
        };

        for (int i = 0; i < testKeys.length; i++) {
            URI uri = URI.create(String.format("http://%s:%d/v1/kv/%s?raw", 
                consulHost, consulPort, testKeys[i]));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            assertEquals(200, response.statusCode(), 
                "Should be able to read seeded key: " + testKeys[i]);
            assertEquals(expectedValues[i], response.body(), 
                "Seeded value should match for key: " + testKeys[i]);
        }
    }

    @Test
    public void testCanWriteAdditionalData() throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        // Write a new value
        String testKey = "test/seed-data-test/dynamic";
        String testValue = "dynamically-added-" + System.currentTimeMillis();
        
        URI putUri = URI.create(String.format("http://%s:%d/v1/kv/%s", 
            consulHost, consulPort, testKey));
        HttpRequest putRequest = HttpRequest.newBuilder()
            .uri(putUri)
            .timeout(Duration.ofSeconds(5))
            .PUT(HttpRequest.BodyPublishers.ofString(testValue))
            .build();

        HttpResponse<String> putResponse = client.send(putRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, putResponse.statusCode(), "Should be able to write new data");

        // Verify it was written
        URI getUri = URI.create(String.format("http://%s:%d/v1/kv/%s?raw", 
            consulHost, consulPort, testKey));
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(getUri)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode(), "Should be able to read new data");
        assertEquals(testValue, getResponse.body(), "New value should match");
    }

}