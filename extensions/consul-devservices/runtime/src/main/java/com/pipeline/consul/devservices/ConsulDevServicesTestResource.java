package com.pipeline.consul.devservices;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test resource that ensures Consul is started before Quarkus application starts.
 * This is critical for integration tests where consul-config needs to connect during startup.
 */
public class ConsulDevServicesTestResource implements QuarkusTestResourceLifecycleManager {
    
    private static final Logger LOG = Logger.getLogger(ConsulDevServicesTestResource.class);
    private static final AtomicReference<Map<String, String>> devServicesProperties = new AtomicReference<>();
    private static volatile boolean started = false;
    private static final TestMode testMode = TestMode.detect();
    
    static {
        // Static initialization ensures Consul starts before Quarkus
        LOG.infof("ConsulDevServicesTestResource static initializer - detected mode: %s", testMode);
        
        // Only start if we're in a mode that requires static initialization
        if (testMode.requiresStaticInit()) {
            String devServicesEnabled = System.getProperty("quarkus.devservices.enabled", "true");
            if (!"false".equals(devServicesEnabled)) {
                startConsulIfNeeded();
            } else {
                LOG.debug("DevServices explicitly disabled");
            }
        } else {
            LOG.debugf("Skipping Consul startup in static init for mode: %s", testMode);
        }
    }
    
    private static void startConsulIfNeeded() {
        // Check if Consul host is already configured
        String consulHost = System.getProperty("quarkus.consul.host");
        if (consulHost != null) {
            LOG.debug("Consul host already configured, skipping DevServices startup");
            return;
        }
        
        try {
            LOG.info("Starting Consul via DevServices for integration tests");
            
            // Create a minimal config - the actual config will come from application properties
            ConsulDevServicesConfig config = new ConsulDevServicesConfig() {
                @Override
                public boolean enabled() {
                    return true;
                }
                
                @Override
                public String imageName() {
                    return System.getProperty("quarkus.consul.devservices.image-name", "hashicorp/consul:1.21");
                }
                
                @Override
                public OptionalInt port() {
                    String port = System.getProperty("quarkus.consul.devservices.port");
                    return port != null ? OptionalInt.of(Integer.parseInt(port)) : OptionalInt.empty();
                }
                
                @Override
                public boolean reuse() {
                    return Boolean.parseBoolean(System.getProperty("quarkus.consul.devservices.reuse", "true"));
                }
                
                @Override
                public String logLevel() {
                    return System.getProperty("quarkus.consul.devservices.log-level", "INFO");
                }
                
                @Override
                public Optional<Map<String, String>> seedData() {
                    return Optional.empty();
                }
                
                @Override
                public Optional<String> consulArgs() {
                    return Optional.empty();
                }
                
                @Override
                public int startupTimeout() {
                    return Integer.parseInt(System.getProperty("quarkus.consul.devservices.startup-timeout", "60"));
                }
                
                @Override
                public String networkAlias() {
                    return System.getProperty("quarkus.consul.devservices.network-alias", "consul");
                }
                
                @Override
                public String networkSubnet() {
                    return System.getProperty("quarkus.consul.devservices.network-subnet", "10.5.0.0/24");
                }
            };
            
            Optional<Map<String, String>> properties = ConsulDevServicesProvider.startConsulContainer(config, LaunchMode.TEST);
            
            if (properties.isPresent()) {
                Map<String, String> props = properties.get();
                devServicesProperties.set(props);
                
                // Set as system properties for the application to pick up
                props.forEach(System::setProperty);
                
                LOG.infof("Consul DevServices started successfully. Consul available at %s:%s",
                    props.get("quarkus.consul.host"),
                    props.get("quarkus.consul.port"));
                
                started = true;
            }
        } catch (Exception e) {
            LOG.error("Failed to start Consul DevServices in static initializer", e);
            throw new RuntimeException("Failed to start Consul DevServices", e);
        }
    }
    
    @Override
    public Map<String, String> start() {
        // Check if this mode should start containers
        if (!testMode.shouldStartContainers()) {
            LOG.debugf("TestMode %s does not require containers", testMode);
            return Collections.emptyMap();
        }
        
        // Return the properties that were set during static initialization
        Map<String, String> props = devServicesProperties.get();
        
        if (props != null) {
            LOG.debug("Returning Consul DevServices properties from static initialization");
            return props;
        }
        
        // For modes that should have containers but don't have properties yet
        // This might happen in DEVELOPMENT mode where static init isn't used
        if (testMode == TestMode.DEVELOPMENT) {
            LOG.debug("Development mode detected, DevServices will handle container startup");
            return Collections.emptyMap();
        }
        
        LOG.warnf("No Consul DevServices properties found for mode: %s", testMode);
        return Collections.emptyMap();
    }
    
    @Override
    public void stop() {
        // Don't stop here - let the DevServices processor handle cleanup
        // This prevents the container from being stopped between tests
        LOG.debug("ConsulDevServicesTestResource.stop() called - delegating to DevServices processor");
    }
    
    @Override
    public int order() {
        // Run early to ensure Consul is available for other resources
        return -1000;
    }
}