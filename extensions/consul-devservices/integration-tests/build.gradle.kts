plugins {
    `java-library`
    id("io.quarkus")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.quarkus.platform:quarkus-bom:3.24.3"))
    
    // Our extension
    implementation(project(":extensions:consul-devservices:runtime"))
    implementation(project(":extensions:consul-devservices:deployment"))
    
    // Test dependencies
    implementation("io.quarkus:quarkus-junit5")
    implementation("io.rest-assured:rest-assured")
    
    // For testing Consul functionality
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")
}