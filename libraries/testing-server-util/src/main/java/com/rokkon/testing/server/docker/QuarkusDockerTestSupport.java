package com.rokkon.testing.server.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Network;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Quarkus-aware Docker test support using the Quarkus Docker Client extension.
 * This provides utilities for working with Docker in tests without directly calling Docker commands.
 */
@ApplicationScoped
public class QuarkusDockerTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(QuarkusDockerTestSupport.class);

    @Inject
    DockerClient dockerClient;

    /**
     * Default constructor for CDI.
     * This class is designed to be instantiated by the CDI container.
     */
    public QuarkusDockerTestSupport() {
        // Default constructor for CDI
    }

    /**
     * Check if Docker is available and running.
     * 
     * @return true if Docker is available and running, false otherwise
     */
    public boolean isDockerAvailable() {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            LOG.debug("Docker is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get information about running containers.
     * 
     * @return a list of ContainerInfo objects representing running containers, or an empty list if an error occurs
     */
    public List<ContainerInfo> getRunningContainers() {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(false) // Only running containers
                .exec();

            return containers.stream()
                .map(c -> new ContainerInfo(
                    c.getId(),
                    c.getNames()[0],
                    c.getImage(),
                    c.getState(),
                    c.getStatus()
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Failed to list containers", e);
            return List.of();
        }
    }

    /**
     * Find a container by name or partial ID.
     * 
     * @param nameOrId the name or partial ID of the container to find
     * @return an Optional containing the ContainerInfo if found, or empty if not found
     */
    public Optional<ContainerInfo> findContainer(String nameOrId) {
        return getRunningContainers().stream()
            .filter(c -> c.name().contains(nameOrId) || c.id().startsWith(nameOrId))
            .findFirst();
    }

    /**
     * Check if a specific container is running.
     * 
     * @param nameOrId the name or partial ID of the container to check
     * @return true if the container is running, false otherwise
     */
    public boolean isContainerRunning(String nameOrId) {
        return findContainer(nameOrId).isPresent();
    }

    /**
     * Get Docker networks.
     * 
     * @return a list of NetworkInfo objects representing Docker networks, or an empty list if an error occurs
     */
    public List<NetworkInfo> getNetworks() {
        try {
            List<Network> networks = dockerClient.listNetworksCmd().exec();
            return networks.stream()
                .map(n -> new NetworkInfo(
                    n.getId(),
                    n.getName(),
                    n.getDriver()
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Failed to list networks", e);
            return List.of();
        }
    }

    /**
     * Container information record.
     * 
     * @param id the container ID
     * @param name the container name
     * @param image the container image
     * @param state the container state
     * @param status the container status
     */
    public record ContainerInfo(
        String id,
        String name,
        String image,
        String state,
        String status
    ) {}

    /**
     * Network information record.
     * 
     * @param id the network ID
     * @param name the network name
     * @param driver the network driver
     */
    public record NetworkInfo(
        String id,
        String name,
        String driver
    ) {}
}

