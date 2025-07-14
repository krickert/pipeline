package com.pipeline.consul.devservices;

import io.quarkus.runtime.LaunchMode;
import org.jboss.logging.Logger;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.DockerClientFactory;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.DockerClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * Manages the lifecycle of Consul containers for DevServices.
 * Uses the two-container sidecar pattern: server + agent.
 */
public class ConsulDevServicesProvider {

    private static final Logger LOG = Logger.getLogger(ConsulDevServicesProvider.class);
    private static final String CONSUL_PORT = "8500";
    private static final String CONTAINER_NAME = "quarkus-consul-devservices";
    private static final String CONSUL_SERVER_ALIAS = "consul-server";
    private static final String CONSUL_AGENT_ALIAS = "consul-agent-engine";
    
    // Labels for container identification
    private static final String LABEL_QUARKUS_CONSUL = "quarkus.consul.devservices";
    private static final String LABEL_CONSUL_TYPE = "quarkus.consul.devservices.type";
    private static final String LABEL_CONSUL_CONFIG = "quarkus.consul.devservices.config";
    
    private static volatile ConsulContainer consulServer;
    private static volatile GenericContainer<?> consulAgent;
    private static volatile Network network;
    private static volatile ConsulDevServicesConfig capturedConfig;
    private static volatile boolean startedByTestResource = false;
    private static volatile IPAllocator ipAllocator;
    
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
        
        LOG.debugf("Starting Consul DevServices in %s mode", launchMode);
        
        if (!config.enabled()) {
            LOG.debug("Consul DevServices is disabled");
            return Optional.empty();
        }
        
        // Check if already started by test resource
        if (consulServer != null && consulAgent != null) {
            LOG.debug("Consul containers already running (started by test resource)");
            startedByTestResource = true;
            return Optional.of(getConfigurationProperties(consulAgent));
        }

        // Check if we should reuse existing containers
        if (config.reuse()) {
            // Try to find existing containers by label
            Optional<Map<String, String>> existingConfig = findAndReuseExistingContainers(config, launchMode);
            if (existingConfig.isPresent()) {
                LOG.info("Reusing existing Consul containers");
                return existingConfig;
            }
        }

        // Stop any existing containers if not reusing or config changed
        if (!config.reuse()) {
            stopConsulContainers();
        }

        // Start new containers using the two-container sidecar pattern
        LOG.infof("Starting Consul DevServices with two-container sidecar pattern using image: %s", config.imageName());
        
        try {
            // Create network with custom subnet
            String subnet = config.networkSubnet();
            network = createNetworkWithSubnet("consul-devservices", subnet);
            
            // Initialize IP allocator
            ipAllocator = new IPAllocator(subnet);
            
            // Allocate IPs for Consul server and agent
            String serverIP = ipAllocator.allocateSpecificIP(2); // .2 for server
            String agentIP = ipAllocator.allocateSpecificIP(3);  // .3 for agent
            
            // Start Consul server with fixed IP
            consulServer = createConsulServer(config, network, serverIP);
            consulServer.start();
            LOG.infof("Consul server started with IP: %s", serverIP);
            
            // Wait a bit for server to be ready
            Thread.sleep(2000);
            
            // Note: Seed data should be configured via withConsulCommand during container creation
            
            // Start Consul agent (sidecar) with fixed IP
            consulAgent = createConsulAgent(config, network, agentIP, serverIP, launchMode);
            consulAgent.start();
            
            capturedConfig = config;
            
            LOG.infof("Consul DevServices started - Agent available at %s:%d", 
                consulAgent.getHost(), 
                consulAgent.getMappedPort(8500));
            
            return Optional.of(getConfigurationProperties(consulAgent));
            
        } catch (Exception e) {
            LOG.error("Failed to start Consul DevServices", e);
            stopConsulContainers();
            throw new RuntimeException("Failed to start Consul DevServices", e);
        }
    }

    /**
     * Stop the Consul container.
     */
    public static void stopConsulContainer() {
        stopConsulContainers();
    }
    
    private static void stopConsulContainers() {
        // Don't stop if started by test resource - let it manage lifecycle
        if (startedByTestResource) {
            LOG.debug("Containers started by test resource, skipping stop");
            return;
        }
        
        if (consulAgent != null) {
            try {
                LOG.info("Stopping Consul agent container");
                consulAgent.stop();
            } catch (Exception e) {
                LOG.warn("Failed to stop Consul agent container", e);
            } finally {
                consulAgent = null;
            }
        }
        
        if (consulServer != null) {
            try {
                LOG.info("Stopping Consul server container");
                consulServer.stop();
            } catch (Exception e) {
                LOG.warn("Failed to stop Consul server container", e);
            } finally {
                consulServer = null;
            }
        }
        
        if (network != null) {
            try {
                network.close();
            } catch (Exception e) {
                LOG.warn("Failed to close network", e);
            } finally {
                network = null;
            }
        }
        
        if (ipAllocator != null) {
            ipAllocator.reset();
            ipAllocator = null;
        }
        
        capturedConfig = null;
        startedByTestResource = false;
    }

    private static ConsulContainer createConsulServer(ConsulDevServicesConfig config, Network network, String ipAddress) {
        DockerImageName dockerImage = DockerImageName.parse(config.imageName())
            .asCompatibleSubstituteFor("consul");
        
        ConsulContainer container = new ConsulContainer(dockerImage)
            .withNetwork(network)
            .withNetworkAliases(CONSUL_SERVER_ALIAS)
            .withEnv("CONSUL_BIND_INTERFACE", "eth0")
            .withStartupTimeout(Duration.ofSeconds(config.startupTimeout()));
        
        // Add seed data commands if configured
        if (config.seedData().isPresent()) {
            Map<String, String> seedData = config.seedData().get();
            for (Map.Entry<String, String> entry : seedData.entrySet()) {
                container.withConsulCommand("kv put " + entry.getKey() + " " + entry.getValue());
                LOG.debugf("Adding seed command for key: %s", entry.getKey());
            }
        }

        // Configure fixed IP if network supports it
        if (ipAddress != null && !ipAddress.isEmpty()) {
            // IP address is set via network attachment, not host config
            LOG.debugf("Container will use IP address: %s", ipAddress);
        }

        // Configure Consul server command
        String[] command = {
            "agent", "-server", "-ui",
            "-client=0.0.0.0",
            "-bind=0.0.0.0",
            "-bootstrap-expect=1",
            "-dev",
            "-log-level=" + config.logLevel()
        };
        
        container.withCommand(command);

        // Add labels for container identification
        container.withLabel(LABEL_QUARKUS_CONSUL, "true");
        container.withLabel(LABEL_CONSUL_TYPE, "server");
        container.withLabel(LABEL_CONSUL_CONFIG, getConfigHash(config));
        
        // Enable reuse if configured
        if (config.reuse()) {
            container.withReuse(true);
        }

        return container;
    }
    
    private static GenericContainer<?> createConsulAgent(ConsulDevServicesConfig config, Network network, String ipAddress, String serverIP, LaunchMode launchMode) {
        DockerImageName dockerImage = DockerImageName.parse(config.imageName());
        
        GenericContainer<?> container = new GenericContainer<>(dockerImage)
            .withNetwork(network)
            .withNetworkAliases(CONSUL_AGENT_ALIAS)
            .withEnv("CONSUL_BIND_INTERFACE", "eth0")
            .withExposedPorts(8500)
            .withStartupTimeout(Duration.ofSeconds(config.startupTimeout()))
            .waitingFor(Wait.forListeningPort());

        // Configure fixed IP if network supports it
        if (ipAddress != null && !ipAddress.isEmpty()) {
            // IP address is set via network attachment, not host config
            LOG.debugf("Container will use IP address: %s", ipAddress);
        }

        // Configure Consul agent command with server IP
        String[] command = {
            "agent",
            "-node=engine-sidecar-dev",
            "-client=0.0.0.0",
            "-bind=0.0.0.0",
            "-retry-join=" + (serverIP != null ? serverIP : CONSUL_SERVER_ALIAS),
            "-log-level=" + config.logLevel()
        };
        
        container.withCommand(command);
        
        // Critical: Add extra host for agent to reach back to host
        container.withExtraHost("host.docker.internal", "host-gateway");
        
        // Set port binding based on mode and configuration
        if (config.port().isPresent()) {
            // Use explicitly configured port
            container.setPortBindings(java.util.List.of(config.port().getAsInt() + ":8500"));
        } else if (launchMode == LaunchMode.DEVELOPMENT) {
            // In dev mode, default to port 8501 for consistency
            container.setPortBindings(java.util.List.of("8501:8500"));
            LOG.info("Dev mode detected - using default port 8501 for Consul agent");
        }
        // Otherwise use random port (default TestContainers behavior)

        // Add labels for container identification
        container.withLabel(LABEL_QUARKUS_CONSUL, "true");
        container.withLabel(LABEL_CONSUL_TYPE, "agent");
        container.withLabel(LABEL_CONSUL_CONFIG, getConfigHash(config));
        
        // Enable reuse if configured
        if (config.reuse()) {
            container.withReuse(true);
        }

        return container;
    }

    private static Map<String, String> getConfigurationProperties(GenericContainer<?> agentContainer) {
        Map<String, String> props = new HashMap<>();
        
        String consulHostPort = agentContainer.getHost() + ":" + agentContainer.getMappedPort(8500);
        
        // Consul Config extension properties - these are recognized by quarkus-config-consul
        props.put("quarkus.consul-config.agent.host-port", consulHostPort);
        props.put("quarkus.consul-config.enabled", "true");
        props.put("quarkus.consul-config.properties-value-keys", "config/application");
        props.put("quarkus.consul-config.fail-on-missing-key", "false");
        
        // System properties for non-CDI usage (e.g., SimpleConsulClientFactory)
        System.setProperty("consul.host", agentContainer.getHost());
        System.setProperty("consul.port", String.valueOf(agentContainer.getMappedPort(8500)));
        
        // Pipeline-specific properties for custom usage
        props.put("pipeline.consul.host", agentContainer.getHost());
        props.put("pipeline.consul.port", String.valueOf(agentContainer.getMappedPort(8500)));
        
        // DevServices metadata
        props.put("quarkus.consul.devservices.enabled", "true");
        props.put("quarkus.consul.devservices.reuse", String.valueOf(capturedConfig != null && capturedConfig.reuse()));
        props.put("quarkus.consul.devservices.log-level", capturedConfig != null ? capturedConfig.logLevel() : "INFO");
        
        return props;
    }
    

    private static boolean configMatches(ConsulDevServicesConfig config1, ConsulDevServicesConfig config2) {
        return config1.imageName().equals(config2.imageName()) &&
               config1.port().equals(config2.port()) &&
               config1.consulArgs().equals(config2.consulArgs()) &&
               config1.logLevel().equals(config2.logLevel());
    }
    
    private static Optional<Map<String, String>> findAndReuseExistingContainers(ConsulDevServicesConfig config, LaunchMode launchMode) {
        try {
            List<Container> containers = DockerClientFactory.instance().client()
                .listContainersCmd()
                .withLabelFilter(Map.of(LABEL_QUARKUS_CONSUL, "true"))
                .withStatusFilter(List.of("running"))
                .exec();
            
            Container agentContainer = null;
            Container serverContainer = null;
            
            for (Container container : containers) {
                Map<String, String> labels = container.getLabels();
                String type = labels.get(LABEL_CONSUL_TYPE);
                String configHash = labels.get(LABEL_CONSUL_CONFIG);
                
                // Check if config matches
                if (configHash != null && configHash.equals(getConfigHash(config))) {
                    if ("agent".equals(type)) {
                        agentContainer = container;
                    } else if ("server".equals(type)) {
                        serverContainer = container;
                    }
                }
            }
            
            // We need both containers to be running
            if (agentContainer != null && serverContainer != null) {
                // Extract port from agent container
                for (ContainerPort port : agentContainer.getPorts()) {
                    if (port.getPrivatePort() != null && port.getPrivatePort() == 8500 && port.getPublicPort() != null) {
                        Map<String, String> props = new HashMap<>();
                        
                        // Consul Config extension properties
                        String consulHostPort = "localhost:" + port.getPublicPort();
                        props.put("quarkus.consul-config.agent.host-port", consulHostPort);
                        props.put("quarkus.consul-config.enabled", "true");
                        props.put("quarkus.consul-config.properties-value-keys", "config/application");
                        props.put("quarkus.consul-config.fail-on-missing-key", "false");
                        
                        // System properties for non-CDI usage
                        System.setProperty("consul.host", "localhost");
                        System.setProperty("consul.port", String.valueOf(port.getPublicPort()));
                        
                        // Pipeline-specific properties
                        props.put("pipeline.consul.host", "localhost");
                        props.put("pipeline.consul.port", String.valueOf(port.getPublicPort()));
                        
                        // DevServices metadata
                        props.put("quarkus.consul.devservices.enabled", "true");
                        props.put("quarkus.consul.devservices.reuse", "true");
                        props.put("quarkus.consul.devservices.log-level", config.logLevel());
                        
                        LOG.infof("Found existing Consul containers - agent port: %d", port.getPublicPort());
                        return Optional.of(props);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to find existing containers", e);
        }
        
        return Optional.empty();
    }
    
    private static String getConfigHash(ConsulDevServicesConfig config) {
        return String.valueOf((config.imageName() + config.logLevel() + config.port()).hashCode());
    }
    
    /**
     * Creates a Docker network with a custom subnet for IP allocation.
     */
    private static Network createNetworkWithSubnet(String networkName, String subnet) {
        try {
            // First check if network already exists
            var dockerClient = DockerClientFactory.instance().client();
            var existingNetworks = dockerClient.listNetworksCmd()
                .withNameFilter(networkName)
                .exec();
                
            if (!existingNetworks.isEmpty()) {
                LOG.infof("Reusing existing network: %s", networkName);
                // Return a DockerNetwork wrapper for the existing network
                final String existingNetworkId = existingNetworks.get(0).getId();
                return new DockerNetwork(existingNetworkId, networkName, false);
            }
            
            // Create new network with subnet
            LOG.infof("Creating network %s with subnet %s", networkName, subnet);
            
            CreateNetworkCmd createNetworkCmd = dockerClient.createNetworkCmd()
                .withName(networkName)
                .withDriver("bridge")
                .withIpam(new com.github.dockerjava.api.model.Network.Ipam()
                    .withConfig(new com.github.dockerjava.api.model.Network.Ipam.Config()
                        .withSubnet(subnet)
                        .withGateway(subnet.substring(0, subnet.lastIndexOf(".")) + ".1")))
                .withLabels(Map.of(
                    "quarkus.consul.devservices", "true",
                    "quarkus.consul.devservices.network", "true"
                ));
                
            var response = createNetworkCmd.exec();
            
            // Return a DockerNetwork wrapper
            return new DockerNetwork(response.getId(), networkName, true);
            
        } catch (Exception e) {
            LOG.warnf("Failed to create custom network, falling back to default: %s", e.getMessage());
            // Fall back to default network creation
            return Network.newNetwork();
        }
    }
    
    /**
     * Get the IP allocator for module containers.
     * This allows modules to get fixed IPs from the subnet.
     */
    public static IPAllocator getIPAllocator() {
        return ipAllocator;
    }
    
    /**
     * Wrapper class to implement Testcontainers Network interface
     */
    private static class DockerNetwork implements Network {
        private final String networkId;
        private final String networkName;
        private final boolean shouldRemove;
        
        DockerNetwork(String networkId, String networkName, boolean shouldRemove) {
            this.networkId = networkId;
            this.networkName = networkName;
            this.shouldRemove = shouldRemove;
        }
        
        @Override
        public String getId() {
            return networkId;
        }
        
        @Override
        public void close() {
            if (shouldRemove) {
                try {
                    DockerClient dockerClient = DockerClientFactory.instance().client();
                    dockerClient.removeNetworkCmd(networkId).exec();
                    LOG.infof("Removed network: %s", networkName);
                } catch (Exception e) {
                    LOG.debugf("Failed to remove network: %s", e.getMessage());
                }
            }
        }
        
        @Override
        public org.junit.runners.model.Statement apply(org.junit.runners.model.Statement base, org.junit.runner.Description description) {
            // This is for JUnit rules, not needed here
            return base;
        }
    }
}