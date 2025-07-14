
package com.rokkon.testing.server.consul;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ConsulTestResource implements QuarkusTestResourceLifecycleManager {

    private GenericContainer<?> consulContainer;
    private boolean containerOwned = false;

    @Override
    public Map<String, String> start() {
        String containerName = ConfigProvider.getConfig().getOptionalValue("rokkon.test.consul.container-name", String.class).orElse("rokkon-test-consul");
        DockerImageName consulImage = DockerImageName.parse("hashicorp/consul:1.21");

        // Create DockerClient manually since we can't use CDI injection in test resources
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        var httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

        Optional<Container> existingContainer = dockerClient.listContainersCmd().withNameFilter(Collections.singleton(containerName)).exec()
                .stream().findFirst();

        if (existingContainer.isPresent()) {
            System.out.println("Attaching to existing Consul container: " + containerName);
            containerOwned = false;
            // This is a simplification. A real implementation would need to inspect the container to get the port.
            // For now, we assume the standard port is exposed on localhost.
            return Map.of(
                "quarkus.consul.host", "localhost",
                "quarkus.consul.port", "8500",
                "consul.host", "localhost",
                "consul.port", "8500",
                "quarkus.consul-config.enabled", "true",
                "quarkus.consul.enabled", "true"
            );
        } else {
            System.out.println("Starting a new Consul container: " + containerName);
            containerOwned = true;
            consulContainer = new GenericContainer<>(consulImage)
                    .withExposedPorts(8500)
                    .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName));
            consulContainer.start();

            return Map.of(
                "quarkus.consul.host", consulContainer.getHost(),
                "quarkus.consul.port", consulContainer.getMappedPort(8500).toString(),
                "consul.host", consulContainer.getHost(),
                "consul.port", consulContainer.getMappedPort(8500).toString(),
                "quarkus.consul-config.enabled", "true",
                "quarkus.consul.enabled", "true"
            );
        }
    }

    @Override
    public void stop() {
        if (containerOwned && consulContainer != null) {
            consulContainer.stop();
        }
    }
}
