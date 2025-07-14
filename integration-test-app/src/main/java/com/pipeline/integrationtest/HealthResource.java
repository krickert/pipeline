package com.pipeline.integrationtest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Simple health check endpoint for testing.
 */
@Path("/api/health")
public class HealthResource {
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "Integration Test App is running!";
    }
}