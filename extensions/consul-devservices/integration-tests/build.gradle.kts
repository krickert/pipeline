plugins {
    `java-library`
    id("io.quarkus")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":bom:pipeline-bom")))
    
    // Our extension
    implementation(project(":extensions:consul-devservices:runtime"))
    implementation(project(":extensions:consul-devservices:deployment"))
    
    // Quarkus dependencies
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    
    // Consul extensions to recognize the configuration
    implementation("io.quarkiverse.config:quarkus-config-consul")
    implementation("io.quarkus:quarkus-smallrye-health")
    
    // Test dependencies
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    
    // Testcontainers for test resources
    testImplementation("org.testcontainers:consul")
    testImplementation("org.testcontainers:testcontainers")
    
    // For testing Consul functionality
    testImplementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")
}