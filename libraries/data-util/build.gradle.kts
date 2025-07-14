plugins {
    `java-library`
    id("io.quarkus")
}

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform(project(":bom:pipeline-bom")))
    implementation(project(":grpc-stubs"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-grpc")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.google.protobuf:protobuf-java-util")
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-io:commons-io")
    implementation("org.slf4j:slf4j-api")

    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation(libs.assertj)
}

tasks.named("compileJava") {
    dependsOn(tasks.named("compileQuarkusGeneratedSourcesJava"))
}

tasks.named("quarkusGenerateCode") {
    enabled = false
}

tasks.named("quarkusGenerateCodeDev") {
    enabled = false
}
