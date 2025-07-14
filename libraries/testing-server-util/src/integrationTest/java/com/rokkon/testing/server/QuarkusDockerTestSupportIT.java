package com.rokkon.testing.server;

import com.rokkon.testing.server.docker.QuarkusDockerTestSupport;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import com.rokkon.testing.server.consul.WithConsul;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for QuarkusDockerTestSupport.
 * These tests verify Docker integration works correctly.
 */
@QuarkusIntegrationTest
@WithConsul
class QuarkusDockerTestSupportIT {
    
    private final QuarkusDockerTestSupport dockerSupport = new QuarkusDockerTestSupport();
    
    @Test
    void testDockerClientCreation() {
        // Verify the Docker support can be instantiated
        assertThat(dockerSupport).isNotNull();
    }
    
    @Test
    @DisabledIfSystemProperty(named = "ci.build", matches = "true")
    void testDockerAvailability() {
        // This test checks if Docker is available
        // It's disabled in CI environments where Docker might not be available
        boolean dockerAvailable = dockerSupport.isDockerAvailable();
        
        // Just log the result - don't fail if Docker isn't available
        if (dockerAvailable) {
            System.out.println("Docker is available for testing");
            
            // If Docker is available, we can test listing containers
            var containers = dockerSupport.getRunningContainers();
            assertThat(containers).isNotNull();
            System.out.println("Found " + containers.size() + " running containers");
            
            // Test listing networks
            var networks = dockerSupport.getNetworks();
            assertThat(networks).isNotNull();
            assertThat(networks).isNotEmpty(); // At least the default networks should exist
            System.out.println("Found " + networks.size() + " Docker networks");
        } else {
            System.out.println("Docker is not available - skipping Docker-specific tests");
        }
    }
}