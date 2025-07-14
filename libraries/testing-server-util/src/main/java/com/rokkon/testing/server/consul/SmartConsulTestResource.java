package com.rokkon.testing.server.consul;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * A Quarkus test resource that conditionally starts a Consul container for tests.
 * This resource will start a Consul container only if the system property "test.consul.real"
 * is set to "true". Otherwise, it will disable Consul configuration in Quarkus.
 * This allows tests to run with or without a real Consul instance.
 */
public class SmartConsulTestResource implements QuarkusTestResourceLifecycleManager {

    private static final boolean USE_REAL_CONSUL =
        Boolean.parseBoolean(System.getProperty("test.consul.real", "false"));

    /**
     * Default constructor for Quarkus test resource.
     * This class is instantiated by the Quarkus test framework.
     */
    public SmartConsulTestResource() {
        // Default constructor for Quarkus test resource
    }

    private static ConsulContainer consulContainer;

    static {
        if (USE_REAL_CONSUL) {
            consulContainer = new ConsulContainer(DockerImageName.parse("hashicorp/consul:1.21"))
                .withConsulCommand("agent", "-dev", "-client=0.0.0.0", "-log-level=info");
            consulContainer.start();
        }
    }

    @Override
    public Map<String, String> start() {
        if (USE_REAL_CONSUL) {
            return Map.of(
                "quarkus.consul-config.enabled", "true",
                "quarkus.consul.enabled", "true",
                "quarkus.consul.host", consulContainer.getHost(),
                "quarkus.consul.port", consulContainer.getMappedPort(8500).toString()
            );
        } else {
            return Map.of(
                "quarkus.consul-config.enabled", "false",
                "quarkus.consul.enabled", "false"
            );
        }
    }

    @Override
    public void stop() {
        if (consulContainer != null) {
            consulContainer.stop();
        }
    }
}
