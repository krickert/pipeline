plugins {
    `java-library`
    id("io.quarkus.extension")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.quarkus.platform:quarkus-bom:3.24.3"))
    
    // Runtime module
    implementation(project(":extensions:consul-devservices:runtime"))
    
    // Deployment dependencies
    implementation("io.quarkus:quarkus-core-deployment")
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-devservices-deployment")
    
    // Test dependencies
    testImplementation("io.quarkus:quarkus-junit5-internal")
}