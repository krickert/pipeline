plugins {
    `java-library`
    id("io.quarkus")
}

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":bom:pipeline-bom")))
    implementation(platform(libs.testcontainers.bom))

    // Migrated project dependencies
    implementation(project(":libraries:data-util"))
    implementation(project(":libraries:pipeline-commons"))
    api(project(":libraries:testing-commons"))  // Keep as api since other modules may need this transitively


    // Docker client libraries (versions managed by Quarkus BOM)
    implementation("com.github.docker-java:docker-java-api")
    implementation("com.github.docker-java:docker-java-core")
    implementation("com.github.docker-java:docker-java-transport-httpclient5")
    
    // Docker and Testcontainers
    implementation("org.testcontainers:testcontainers")
    implementation("org.testcontainers:junit-jupiter")
    implementation("org.testcontainers:consul")
    implementation("org.testcontainers:postgresql")
    implementation("org.testcontainers:kafka")

    // Quarkus test utilities
    implementation("io.quarkus:quarkus-junit5")
    implementation("io.quarkus:quarkus-test-common")
    implementation("io.quarkus:quarkus-test-vertx")
    
    // gRPC testing utilities
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.grpc:grpc-testing")
    
    // Additional testing utilities
    implementation("org.awaitility:awaitility")
    implementation(libs.assertj)
    implementation("org.mockito:mockito-core")
    implementation("org.mockito:mockito-junit-jupiter")
}
