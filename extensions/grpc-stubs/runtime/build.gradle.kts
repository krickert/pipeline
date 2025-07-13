plugins {
    id("io.quarkus.extension")
    id("io.quarkus")
}

quarkusExtension {
    deploymentModule.set("grpc-stubs-deployment")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    annotationProcessor("io.quarkus:quarkus-extension-processor:${quarkusPlatformVersion}")
    api("io.quarkus:quarkus-core")
    api("io.quarkus:quarkus-arc")
    
    // gRPC dependencies
    api("io.quarkus:quarkus-grpc") {
        exclude(group = "io.grpc", module = "grpc-testing-proto")
    }
    api("io.quarkus:quarkus-mutiny")
    api("io.quarkus:quarkus-cache")
    api("io.smallrye.stork:stork-api")
    api("com.google.api.grpc:proto-google-common-protos")
    
    // Our protobuf definitions
    api("com.pipeline:protobuf:1.0.0-SNAPSHOT")
    
    // Utilities
    implementation("org.apache.commons:commons-lang3")
}