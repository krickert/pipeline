plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":bom:pipeline-bom")))
    
    // Runtime module
    implementation(project(":extensions:consul-devservices:runtime"))
    
    // Deployment dependencies
    implementation("io.quarkus:quarkus-core-deployment")
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-devservices-deployment")
    
    // Consul configuration deployment - only for DevServices
    compileOnly("io.quarkiverse.config:quarkus-config-consul-deployment:2.4.0")
    
    // Test dependencies
    testImplementation("io.quarkus:quarkus-junit5-internal")
}