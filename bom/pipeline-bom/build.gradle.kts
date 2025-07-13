plugins {
    `java-platform`
    `maven-publish`
}

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

javaPlatform {
    allowDependencies()
}

dependencies {
    // Import Quarkus BOM
    api(platform("io.quarkus.platform:quarkus-bom:${property("quarkusPlatformVersion")}"))
    
    // Additional constraints for dependencies not in Quarkus BOM
    constraints {
        // Consul
        api("com.orbitz.consul:consul-client:1.6.0")
        
        // Any other non-Quarkus managed dependencies would go here
    }
    
    // Re-export key Quarkus dependencies for convenience
    api("io.quarkus:quarkus-core")
    api("io.quarkus:quarkus-arc")
    api("io.quarkus:quarkus-grpc")
    api("io.quarkus:quarkus-rest")
    api("io.quarkus:quarkus-rest-jackson")
    api("io.quarkus:quarkus-config-yaml")
    
    // gRPC dependencies (versions from Quarkus BOM)
    api("io.grpc:grpc-netty")
    api("io.grpc:grpc-protobuf")
    api("io.grpc:grpc-stub")
    api("io.grpc:grpc-services")
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")
    
    // Testing dependencies (versions from Quarkus BOM)
    api("io.quarkus:quarkus-junit5")
    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core")
    api("io.rest-assured:rest-assured")
    api("org.testcontainers:testcontainers")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])
        }
    }
}