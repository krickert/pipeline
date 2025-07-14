package com.pipeline.consul.devservices.it;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Test resource that starts Consul in a static initializer to ensure it's available
 * before Quarkus tries to validate configuration. This solves the chicken-and-egg
 * problem with consul-config needing Consul to be running.
 * 
 * This is a simpler alternative to ConsulDevServicesTestResource that uses a single
 * container instead of the two-container sidecar pattern.
 */
public class ConsulStaticTestResource implements QuarkusTestResourceLifecycleManager {
    
    private static final Logger LOG = Logger.getLogger(ConsulStaticTestResource.class.getName());
    private static final ConsulContainer consulContainer;
    
    static {
        LOG.info("Starting Consul container in static initializer...");
        
        boolean reuse = Boolean.parseBoolean(System.getProperty("testcontainers.reuse.enable", "true"));
        consulContainer = new ConsulContainer(DockerImageName.parse("hashicorp/consul:1.21"))
            .withReuse(reuse);
        
        consulContainer.start();
        
        // Set system properties immediately so they're available during Quarkus startup
        System.setProperty("consul.host", consulContainer.getHost());
        System.setProperty("consul.port", String.valueOf(consulContainer.getMappedPort(8500)));
        System.setProperty("quarkus.consul-config.agent.host-port", 
            consulContainer.getHost() + ":" + consulContainer.getMappedPort(8500));
        System.setProperty("pipeline.consul.host", consulContainer.getHost());
        System.setProperty("pipeline.consul.port", String.valueOf(consulContainer.getMappedPort(8500)));
        
        LOG.info("Consul started at: " + consulContainer.getHost() + ":" + consulContainer.getMappedPort(8500));
    }
    
    @Override
    public Map<String, String> start() {
        // Return the same values we set as system properties
        // This ensures they're available in multiple ways
        return Map.of(
            "quarkus.consul-config.enabled", "true",
            "quarkus.consul-config.agent.host-port", 
                consulContainer.getHost() + ":" + consulContainer.getMappedPort(8500),
            "quarkus.consul-config.fail-on-missing-key", "false",
            // Pipeline-specific properties
            "pipeline.consul.host", consulContainer.getHost(),
            "pipeline.consul.port", consulContainer.getMappedPort(8500).toString()
        );
    }
    
    @Override
    public void stop() {
        // If reuse is disabled, Testcontainers will automatically stop the container
        // after the test run. If reuse is enabled, it will be stopped on JVM shutdown.
    }
}