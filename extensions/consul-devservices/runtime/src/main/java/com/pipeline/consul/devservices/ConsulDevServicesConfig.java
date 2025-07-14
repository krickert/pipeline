package com.pipeline.consul.devservices;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Map;
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
    @WithDefault("true")
    boolean enabled();

    /**
     * The container image name to use for Consul.
     */
    @WithDefault("hashicorp/consul:1.21")
    String imageName();

    /**
     * Optional fixed port for Consul.
     * If not defined, a random port will be used.
     */
    OptionalInt port();

    /**
     * Indicates if containers should be reused between runs.
     * Useful in dev mode to avoid recreating containers on every restart.
     */
    @WithDefault("true")
    boolean reuse();

    /**
     * Log level for Consul containers.
     * Valid values are: TRACE, DEBUG, INFO, WARN, ERR
     */
    @WithDefault("INFO")
    String logLevel();

    /**
     * Initial key-value pairs to seed in Consul on startup.
     * Format: key=value
     */
    Optional<Map<String, String>> seedData();

    /**
     * Additional arguments to pass to Consul.
     */
    Optional<String> consulArgs();

    /**
     * Startup timeout in seconds.
     */
    @WithDefault("60")
    int startupTimeout();

    /**
     * Network alias for the Consul server container.
     */
    @WithDefault("consul")
    String networkAlias();

    /**
     * Network subnet to use for Consul containers.
     * This helps avoid conflicts with other Docker networks.
     */
    @WithDefault("10.5.0.0/24")
    String networkSubnet();
}