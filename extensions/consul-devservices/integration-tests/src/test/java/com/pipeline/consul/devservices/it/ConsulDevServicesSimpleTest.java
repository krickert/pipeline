package com.pipeline.consul.devservices.it;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify Consul DevServices starts using the single-container static resource.
 */
@QuarkusTest
@WithTestResource(ConsulStaticTestResource.class)
public class ConsulDevServicesSimpleTest {

    @ConfigProperty(name = "quarkus.consul-config.agent.host-port", defaultValue = "not-set")
    String consulConfigHostPort;
    
    @ConfigProperty(name = "consul.host", defaultValue = "not-set")
    String consulHost;
    
    @ConfigProperty(name = "consul.port", defaultValue = "0")
    String consulPort;
    
    @ConfigProperty(name = "pipeline.consul.host", defaultValue = "not-set")
    String pipelineConsulHost;
    
    @ConfigProperty(name = "pipeline.consul.port", defaultValue = "0")
    String pipelineConsulPort;

    @Test
    public void testConsulDevServicesConfigured() {
        System.out.println("consul-config.agent.host-port: " + consulConfigHostPort);
        System.out.println("consul.host: " + consulHost);
        System.out.println("consul.port: " + consulPort);
        System.out.println("pipeline.consul.host: " + pipelineConsulHost);
        System.out.println("pipeline.consul.port: " + pipelineConsulPort);
        
        assertNotEquals("not-set", consulConfigHostPort, "Consul config host-port should be set by DevServices");
        assertNotEquals("not-set", consulHost, "Consul host should be set by DevServices");
        assertNotEquals("0", consulPort, "Consul port should be set by DevServices");
        assertNotEquals("not-set", pipelineConsulHost, "Pipeline consul host should be set by DevServices");
        assertNotEquals("0", pipelineConsulPort, "Pipeline consul port should be set by DevServices");
    }
}