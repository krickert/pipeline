package com.pipeline.consul.devservices;

import io.quarkus.runtime.LaunchMode;
import org.jboss.logging.Logger;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the lifecycle of Consul containers for DevServices.
 * 
 * This is a simplified version that uses single container mode by default.
 * The two-container sidecar pattern can be added later.
 */
public class ConsulDevServicesProvider {

    private static final Logger LOG = Logger.getLogger(ConsulDevServicesProvider.class);
    private static final String CONSUL_PORT = "8500";
    private static final String CONTAINER_NAME = "quarkus-consul-devservices";
    
    private static volatile ConsulContainer consulServer;
    private static volatile ConsulDevServicesConfig capturedConfig;
    
    /**
     * Start the Consul container if needed.
     * 
     * @param config The DevServices configuration
     * @param launchMode The current launch mode (DEV, TEST, etc.)
     * @return Configuration properties to inject, or empty if Consul was not started
     */
    public static Optional<Map<String, String>> startConsulContainer(
            ConsulDevServicesConfig config, 
            LaunchMode launchMode) {
        
        if (!config.enabled()) {
            LOG.debug("Consul DevServices is disabled");
            return Optional.empty();
        }
        
        // Check if already started
        if (consulServer != null && consulServer.isRunning()) {
            LOG.debug("Consul container already running");
            return Optional.of(getConfigurationProperties());
        }

        // Start new container
        LOG.infof("Starting Consul DevServices using image: %s", config.imageName());
        
        try {
            DockerImageName dockerImage = DockerImageName.parse(config.imageName())
                    .asCompatibleSubstituteFor("consul");
            
            consulServer = new ConsulContainer(dockerImage)
                    .withConsulCommand("agent -dev -client 0.0.0.0 -log-level=" + config.logLevel());
            
            // Configure fixed port if specified
            if (config.port().isPresent()) {
                consulServer.withExposedPorts(config.port().getAsInt());
            }
            
            // Enable UI if configured
            if (config.enableUi()) {
                consulServer.withConsulCommand(consulServer.getCommandParts()[0] + " -ui");
            }
            
            // Start the container
            consulServer.start();
            
            capturedConfig = config;
            
            LOG.infof("Consul DevServices started at %s:%d", 
                    consulServer.getHost(), 
                    consulServer.getMappedPort(8500));
            
            // Seed initial data if configured
            seedInitialData(config);
            
            return Optional.of(getConfigurationProperties());
            
        } catch (Exception e) {
            LOG.error("Failed to start Consul DevServices", e);
            stopConsulContainer();
            return Optional.empty();
        }
    }
    
    /**
     * Stop the Consul container.
     */
    public static void stopConsulContainer() {
        if (consulServer != null) {
            try {
                LOG.info("Stopping Consul DevServices container");
                consulServer.stop();
            } catch (Exception e) {
                LOG.error("Failed to stop Consul container", e);
            } finally {
                consulServer = null;
            }
        }
    }
    
    /**
     * Get the configuration properties for the running Consul container.
     */
    private static Map<String, String> getConfigurationProperties() {
        Map<String, String> properties = new HashMap<>();
        
        if (consulServer != null && consulServer.isRunning()) {
            properties.put("quarkus.consul-config.agent.host", consulServer.getHost());
            properties.put("quarkus.consul-config.agent.port", String.valueOf(consulServer.getMappedPort(8500)));
            
            // Also set the legacy properties for compatibility
            properties.put("consul.host", consulServer.getHost());
            properties.put("consul.port", String.valueOf(consulServer.getMappedPort(8500)));
            
            // Pipeline-specific properties
            properties.put("pipeline.consul.host", consulServer.getHost());
            properties.put("pipeline.consul.port", String.valueOf(consulServer.getMappedPort(8500)));
        }
        
        return properties;
    }
    
    /**
     * Seed initial key-value data if configured.
     */
    private static void seedInitialData(ConsulDevServicesConfig config) {
        if (config.seedData().isPresent()) {
            LOG.info("Seeding initial Consul data");
            // TODO: Implement seeding logic using Consul client
            // This would create initial keys/values in Consul
        }
    }
}