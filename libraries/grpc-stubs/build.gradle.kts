plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.4"
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    // Quarkus BOM
    implementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    testImplementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    
    // Our protobuf definitions
    api(project(":protobuf"))
    
    // gRPC dependencies
    api("io.quarkus:quarkus-grpc")
    api("io.quarkus:quarkus-mutiny")
    api("io.quarkus:quarkus-cache")
    api("io.smallrye.stork:stork-api")
    api("com.google.api.grpc:proto-google-common-protos")
    
    // Utilities
    implementation("org.apache.commons:commons-lang3")
    
    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}


protobuf {
    protoc {
        // Use protoc version from Quarkus BOM
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        // Standard gRPC Java plugin
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.68.1"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}