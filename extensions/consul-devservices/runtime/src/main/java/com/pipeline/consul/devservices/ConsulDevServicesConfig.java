package com.pipeline.consul.devservices;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Configuration for Consul DevServices.
 */
@ConfigMapping(prefix = "quarkus.consul.devservices")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ConsulDevServicesConfig {

    /**
     * If DevServices has been explicitly enabled or disabled.
     * DevServices is enabled by default in dev and test mode.
     */
    @ConfigItem(defaultValue = "true")
    boolean enabled();

    /**
     * The container image name to use for Consul.
     */
    @ConfigItem(defaultValue = "hashicorp/consul:1.21")
    String imageName();

    /**
     * Optional fixed port for Consul.
     * If not defined, a random port will be used.
     */
    @ConfigItem
    OptionalInt port();

    /**
     * The port for the agent to use on the host.
     * This simulates the sidecar pattern where the agent runs on the host network.
     */
    @ConfigItem(defaultValue = "8501")
    int agentPort();

    /**
     * Indicates if containers should be reused between runs.
     * Useful in dev mode to avoid recreating containers on every restart.
     */
    @ConfigItem(defaultValue = "true")
    boolean reuse();

    /**
     * Log level for Consul containers.
     * Valid values are: TRACE, DEBUG, INFO, WARN, ERR
     */
    @ConfigItem(defaultValue = "INFO")
    String logLevel();

    /**
     * Whether to use a custom network for Consul containers.
     */
    @ConfigItem(defaultValue = "true")
    boolean useCustomNetwork();

    /**
     * Network subnet to use for Consul containers.
     * This helps avoid conflicts with other Docker networks.
     */
    @ConfigItem(defaultValue = "172.28.0.0/16")
    String networkSubnet();

    /**
     * Whether to enable the Consul UI.
     */
    @ConfigItem(defaultValue = "true")
    boolean enableUi();

    /**
     * Initial key-value pairs to seed in Consul on startup.
     * Format: key=value
     */
    @ConfigItem
    Optional<String[]> seedData();

    /**
     * Whether to run in single container mode (just server) or 
     * two-container sidecar pattern (server + agent).
     */
    @ConfigItem(defaultValue = "false")
    boolean singleContainerMode();
}