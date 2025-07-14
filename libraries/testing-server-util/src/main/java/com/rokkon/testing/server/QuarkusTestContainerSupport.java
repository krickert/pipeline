package com.rokkon.testing.server;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class providing common Testcontainers setup for Quarkus server integration tests.
 * This class offers utility methods to create and configure containers for testing
 * Quarkus applications with common dependencies like PostgreSQL and Kafka.
 */
public abstract class QuarkusTestContainerSupport {

    /** Shared network for connecting test containers */
    protected static final Network TEST_NETWORK = Network.newNetwork();

    /**
     * Protected constructor for subclasses.
     * This abstract class is designed to be extended by concrete test support classes.
     */
    protected QuarkusTestContainerSupport() {
        // Protected constructor for subclasses
    }

    /**
     * Creates a PostgreSQL container configured for Quarkus testing.
     * 
     * @return a configured PostgreSQL container connected to the test network
     */
    protected static PostgreSQLContainer<?> createPostgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withNetwork(TEST_NETWORK)
                .withNetworkAliases("postgres")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test");
    }

    /**
     * Creates a Kafka container configured for Quarkus testing.
     * 
     * @return a configured Kafka container connected to the test network
     */
    protected static KafkaContainer createKafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))
                .withNetwork(TEST_NETWORK)
                .withNetworkAliases("kafka");
    }

    /**
     * Creates a generic container for a Quarkus application.
     * 
     * @param imageName the Docker image name for the Quarkus application
     * @return a configured GenericContainer connected to the test network with standard ports exposed
     */
    protected static GenericContainer<?> createQuarkusAppContainer(String imageName) {
        return new GenericContainer<>(DockerImageName.parse(imageName))
                .withNetwork(TEST_NETWORK)
                .withExposedPorts(8080, 9000) // HTTP and gRPC ports
                .withEnv("QUARKUS_PROFILE", "test");
    }
}
