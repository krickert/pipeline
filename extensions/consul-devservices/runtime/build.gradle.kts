plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.quarkus.platform:quarkus-bom:3.24.3"))
    
    // Core Quarkus dependencies
    implementation("io.quarkus:quarkus-core")
    implementation("io.quarkus:quarkus-arc")
    
    // Consul configuration from Quarkiverse
    implementation("io.quarkiverse.config:quarkus-config-consul")
    
    // Vert.x Mutiny Consul client for KV operations
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")
    
    // Testcontainers for DevServices
    implementation("org.testcontainers:consul:1.19.8")
    implementation("io.quarkus:quarkus-devservices-common")
    
    // Test support - compileOnly to avoid runtime dependency
    compileOnly("io.quarkus:quarkus-test-common")
    
    // Logging
    implementation("org.jboss.logging:jboss-logging")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "pipeline-consul-devservices"
        }
    }
}