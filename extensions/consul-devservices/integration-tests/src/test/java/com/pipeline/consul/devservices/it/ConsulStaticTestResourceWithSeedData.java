package com.pipeline.consul.devservices.it;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Test resource that starts Consul with seed data support.
 */
public class ConsulStaticTestResourceWithSeedData implements QuarkusTestResourceLifecycleManager {
    
    private static final Logger LOG = Logger.getLogger(ConsulStaticTestResourceWithSeedData.class.getName());
    private static final ConsulContainer consulContainer;
    
    static {
        LOG.info("Starting Consul container with seed data support...");
        
        boolean reuse = Boolean.parseBoolean(System.getProperty("testcontainers.reuse.enable", "true"));
        consulContainer = new ConsulContainer(DockerImageName.parse("hashicorp/consul:1.21"))
            .withReuse(reuse)
            // Add seed data commands before starting
            .withConsulCommand("kv put config/application/pipeline.engine.name pipeline-engine-test")
            .withConsulCommand("kv put config/application/pipeline.engine.version 1.0.0-TEST")
            .withConsulCommand("kv put config/test/quarkus.http.port 39001")
            .withConsulCommand("kv put config/test/quarkus.grpc.server.port 49001")
            .withConsulCommand("kv put config/test/pipeline.engine.test-mode true");
        
        LOG.info("Starting Consul with seed data...");
        consulContainer.start();
        
        // Set system properties immediately
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
        return Map.of(
            "quarkus.consul-config.enabled", "true",
            "quarkus.consul-config.agent.host-port", 
                consulContainer.getHost() + ":" + consulContainer.getMappedPort(8500),
            "quarkus.consul-config.fail-on-missing-key", "false",
            "pipeline.consul.host", consulContainer.getHost(),
            "pipeline.consul.port", consulContainer.getMappedPort(8500).toString()
        );
    }
    
    @Override
    public void stop() {
        // Container lifecycle managed by reuse feature
    }
}