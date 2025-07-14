package com.pipeline.consul.devservices;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides Consul agent sidecars for pipeline modules.
 * Each module gets its own Consul agent sidecar with a fixed IP.
 */
public class ModuleSidecarProvider {
    
    private static final Logger LOG = Logger.getLogger(ModuleSidecarProvider.class);
    
    /**
     * Create a Consul agent sidecar for a module.
     * 
     * @param moduleName The name of the module
     * @param network The Docker network to use
     * @param ipAllocator The IP allocator for getting fixed IPs
     * @param consulServerIP The IP of the Consul server
     * @param consulImage The Consul Docker image to use
     * @param logLevel The log level for Consul
     * @return The configured sidecar container
     */
    public static GenericContainer<?> createModuleSidecar(
            String moduleName,
            Network network,
            IPAllocator ipAllocator,
            String consulServerIP,
            String consulImage,
            String logLevel) {
        
        // Allocate an IP for this module's sidecar
        String sidecarIP = ipAllocator.allocateIP();
        LOG.infof("Allocated IP %s for module %s sidecar", sidecarIP, moduleName);
        
        DockerImageName dockerImage = DockerImageName.parse(consulImage);
        
        GenericContainer<?> container = new GenericContainer<>(dockerImage)
            .withNetwork(network)
            .withNetworkAliases("consul-agent-" + moduleName)
            .withEnv("CONSUL_BIND_INTERFACE", "eth0")
            .withExposedPorts(8500)
            .withStartupTimeout(Duration.ofSeconds(60))
            .waitingFor(Wait.forListeningPort());
        
        // Configure Consul agent command
        String[] command = {
            "agent",
            "-node=" + moduleName + "-sidecar",
            "-client=0.0.0.0",
            "-bind=0.0.0.0",
            "-retry-join=" + consulServerIP,
            "-log-level=" + logLevel
        };
        
        container.withCommand(command);
        
        // Add labels for identification
        container.withLabel("quarkus.consul.devservices", "true");
        container.withLabel("quarkus.consul.devservices.type", "module-sidecar");
        container.withLabel("quarkus.consul.devservices.module", moduleName);
        
        // Enable reuse
        container.withReuse(true);
        
        return container;
    }
    
    /**
     * Create a module container that uses the sidecar's network namespace.
     * This allows the module to connect to Consul on localhost:8500.
     * 
     * @param moduleImage The module's Docker image
     * @param moduleName The module name
     * @param sidecarContainer The sidecar container to share network with
     * @param environment Additional environment variables
     * @param memoryLimit Optional memory limit in MB
     * @return The configured module container
     */
    public static GenericContainer<?> createModuleWithSidecar(
            String moduleImage,
            String moduleName,
            GenericContainer<?> sidecarContainer,
            Map<String, String> environment,
            Integer memoryLimit) {
        
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(moduleImage))
            .withNetworkMode("container:" + sidecarContainer.getContainerId())
            .withEnv(environment)
            .withStartupTimeout(Duration.ofSeconds(120));
        
        // Set memory limit if specified
        if (memoryLimit != null && memoryLimit > 0) {
            container.withCreateContainerCmdModifier(cmd -> 
                cmd.getHostConfig().withMemory(memoryLimit * 1024L * 1024L));
        }
        
        // Add labels
        container.withLabel("quarkus.consul.devservices", "true");
        container.withLabel("quarkus.consul.devservices.type", "module");
        container.withLabel("quarkus.consul.devservices.module", moduleName);
        
        // Enable reuse
        container.withReuse(true);
        
        return container;
    }
    
    /**
     * Get the configuration properties for a module using a sidecar.
     * 
     * @param moduleName The module name
     * @return Configuration properties for the module
     */
    public static Map<String, String> getModuleConfiguration(String moduleName) {
        Map<String, String> props = new HashMap<>();
        
        // Module connects to Consul via localhost in shared network namespace
        props.put("consul.host", "localhost");
        props.put("consul.port", "8500");
        props.put("pipeline.consul.host", "localhost");
        props.put("pipeline.consul.port", "8500");
        
        // Module-specific properties
        props.put("module.name", moduleName);
        props.put("consul.service.name", moduleName);
        
        return props;
    }
}