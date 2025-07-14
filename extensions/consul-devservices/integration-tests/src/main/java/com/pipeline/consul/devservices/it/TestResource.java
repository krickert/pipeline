package com.pipeline.consul.devservices.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/test")
public class TestResource {

    @ConfigProperty(name = "pipeline.consul.host", defaultValue = "not-set")
    String consulHost;

    @ConfigProperty(name = "pipeline.consul.port", defaultValue = "0")
    int consulPort;

    @GET
    @Path("/consul-config")
    @Produces(MediaType.APPLICATION_JSON)
    public ConsulConfig getConsulConfig() {
        return new ConsulConfig(consulHost, consulPort);
    }

    public static record ConsulConfig(String host, int port) {}
}