plugins {
    `java-library`
    id("io.quarkus")
}

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

// This application serves as the integration test environment for the pipeline project.
// It provides a running Quarkus application for testing extensions and libraries
// without requiring the full engine infrastructure.

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":bom:pipeline-bom")))
    implementation(project(":grpc-stubs"))
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-rest")
    
    // For testing Consul integration
    implementation("io.quarkus:quarkus-smallrye-health")
    
    // Test dependencies
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    
    // Integration test dependencies
    integrationTestImplementation(project(":extensions:consul-devservices:runtime"))
    integrationTestImplementation("io.quarkus:quarkus-junit5")
    integrationTestImplementation("io.rest-assured:rest-assured")
    integrationTestImplementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")
}