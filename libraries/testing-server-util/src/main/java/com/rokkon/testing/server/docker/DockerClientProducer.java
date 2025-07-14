package com.rokkon.testing.server.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * CDI producer for DockerClient instances.
 * This provides a properly configured DockerClient that can be injected throughout the application.
 */
@ApplicationScoped
public class DockerClientProducer {
    private static final Logger LOG = LoggerFactory.getLogger(DockerClientProducer.class);

    /**
     * Produces a singleton DockerClient instance.
     * The client is configured with default settings and HTTP client with reasonable timeouts.
     *
     * @return a configured DockerClient instance
     */
    @Produces
    @Singleton
    public DockerClient produceDockerClient() {
        LOG.debug("Creating DockerClient instance");
        
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
            
        LOG.info("DockerClient created successfully");
        return dockerClient;
    }
}