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
    api(project(":libraries:pipeline-commons"))
    api(project(":libraries:data-util"))
    implementation(project(":grpc-stubs"))
    implementation(project(":libraries:pipeline-api"))
    implementation(project(":libraries:validators"))
    implementation("io.quarkus:quarkus-mutiny")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.google.protobuf:protobuf-java-util")

    // Test frameworks
    implementation("org.junit.jupiter:junit-jupiter-api")
    implementation(libs.assertj)

    // Testcontainers
    implementation(platform(libs.testcontainers.bom))
    implementation("org.testcontainers:testcontainers")
    implementation("org.testcontainers:junit-jupiter")

    // Quarkus test support
    implementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5")
}