plugins {
    `java-library`
    id("io.quarkus")
    id("org.kordamp.gradle.jandex") version "1.1.0"
}

// Configure gRPC code generation
quarkus {
    quarkusBuildProperties.put("quarkus.generate-code.grpc.scan-for-imports", "com.google.protobuf:protobuf-java,com.google.api.grpc:proto-google-common-protos")
    quarkusBuildProperties.put("quarkus.generate-code.grpc.scan-for-proto", "com.google.api.grpc:proto-google-common-protos")
}

// Exclude Google proto packages from the JAR to avoid split packages
tasks.jar {
    exclude("com/google/**")
}

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":bom:pipeline-bom")))
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-mutiny")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.google.api.grpc:proto-google-common-protos")
}
