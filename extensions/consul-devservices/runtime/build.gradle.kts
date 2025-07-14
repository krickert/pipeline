plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":bom:pipeline-bom")))
    
    // Core Quarkus dependencies
    implementation("io.quarkus:quarkus-core")
    implementation("io.quarkus:quarkus-arc")
    
    // Consul configuration from Quarkiverse - only for DevServices
    compileOnly("io.quarkiverse.config:quarkus-config-consul:2.4.0")
    
    // Vert.x Mutiny Consul client for KV operations
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")
    
    // Testcontainers for DevServices - should only be available during dev/test
    implementation("org.testcontainers:consul")
    implementation("org.testcontainers:testcontainers")
    implementation("io.quarkus:quarkus-devservices-common")
    
    // Test support - compileOnly to avoid runtime dependency
    compileOnly("io.quarkus:quarkus-test-common")
    
    // Test framework support for ConsulQuarkusIntegrationTest
    implementation("org.junit.jupiter:junit-jupiter-api")
    implementation("io.quarkus:quarkus-junit5")
    
    // Jackson for ObjectMapper (in ConsulTestContext)
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Pipeline utilities for ObjectMapperFactory
    implementation(project(":libraries:pipeline-commons"))
    
    // Docker client - only needed for DevServices
    implementation("com.github.docker-java:docker-java-api:3.3.6")
    
    // Logging
    implementation("org.jboss.logging:jboss-logging")
}