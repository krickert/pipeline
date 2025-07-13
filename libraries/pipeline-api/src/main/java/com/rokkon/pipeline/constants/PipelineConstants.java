package com.rokkon.pipeline.constants;

/**
 * Common constants used across the pipeline system.
 * These constants ensure consistency and avoid hardcoded values.
 */
public final class PipelineConstants {
    
    private PipelineConstants() {
        // Utility class
    }
    
    // Application defaults
    public static final String DEFAULT_APP_NAME = "pipeline-engine";
    
    // Consul key patterns
    public static final String CONSUL_KEY_SEPARATOR = "/";
    public static final String CONSUL_CLUSTERS_KEY = "clusters";
    public static final String CONSUL_PIPELINES_KEY = "pipelines";
    public static final String CONSUL_MODULES_KEY = "modules";
    public static final String CONSUL_CONFIG_KEY = "config";
    public static final String CONSUL_DEFINITIONS_KEY = "definitions";
    public static final String CONSUL_REGISTERED_KEY = "registered";
    public static final String CONSUL_ENABLED_MODULES_KEY = "enabled-modules";
    
    // Module deployment constants
    public static final String MODULE_LABEL_PREFIX = "pipeline.";
    public static final String MODULE_LABEL_NAME = MODULE_LABEL_PREFIX + "module";
    public static final String MODULE_LABEL_TYPE = MODULE_LABEL_PREFIX + "type";
    public static final String MODULE_LABEL_PORT = MODULE_LABEL_PREFIX + "port";
    
    // Module types
    public static final String MODULE_TYPE_CONSUL_SIDECAR = "consul-sidecar";
    public static final String MODULE_TYPE_MODULE = "module";
    public static final String MODULE_TYPE_REGISTRATION_SIDECAR = "registration-sidecar";
    
    // Network constants
    public static final String NETWORK_SUFFIX = "-network";
    public static final String NETWORK_DRIVER = "bridge";
    
    // Container name patterns
    public static final String CONSUL_AGENT_PREFIX = "consul-agent-";
    public static final String MODULE_APP_SUFFIX = "-module-app";
    public static final String REGISTRAR_SUFFIX = "-registrar";
    
    // Port allocation
    public static final int DEV_PORT_RANGE_START = 39100;
    public static final int ENGINE_DEFAULT_PORT = 38081;
    public static final int CONSUL_SERVER_PORT = 38500;
    public static final int CONSUL_AGENT_PORT = 8501;
    
    // Environment variables
    public static final String ENV_MODULE_NAME = "MODULE_NAME";
    public static final String ENV_MODULE_PORT = "MODULE_PORT";
    public static final String ENV_ENGINE_HOST = "ENGINE_HOST";
    public static final String ENV_ENGINE_PORT = "ENGINE_PORT";
    public static final String ENV_AUTO_REGISTER = "AUTO_REGISTER";
    public static final String ENV_REGISTRATION_HOST = "REGISTRATION_HOST";
    public static final String ENV_REGISTRATION_PORT = "REGISTRATION_PORT";
    
    // Docker images
    public static final String CONSUL_IMAGE = "hashicorp/consul:1.21";
    public static final String DEFAULT_MODULE_IMAGE_PREFIX = "pipeline/";
    
    // CLI paths
    public static final String PIPELINE_CLI_JAR = "/deployments/pipeline-cli.jar";
    
    // Timing constants
    public static final int MODULE_STARTUP_DELAY_SECONDS = 15;
    
    /**
     * Build a Consul key path with the application name prefix.
     * 
     * @param appName The application name
     * @param pathSegments The path segments to join
     * @return The full Consul key path
     */
    public static String buildConsulKey(String appName, String... pathSegments) {
        StringBuilder key = new StringBuilder(appName);
        for (String segment : pathSegments) {
            key.append(CONSUL_KEY_SEPARATOR).append(segment);
        }
        return key.toString();
    }
}