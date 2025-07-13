rootProject.name = "pipeline-project"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
    }
}

// Version catalog is automatically loaded from gradle/libs.versions.toml

// Bill of Materials
includeBuild("bom/pipeline-bom")

// Pure Java Libraries
// includeBuild("libraries/protobuf")
// includeBuild("libraries/testing-commons")

// Quarkus Libraries
// includeBuild("quarkus-libraries/pipeline-api")
// includeBuild("quarkus-libraries/pipeline-commons")
// includeBuild("quarkus-libraries/consul-client")

// Extensions
// includeBuild("extensions/grpc-stubs")
// includeBuild("extensions/dynamic-grpc")
// includeBuild("extensions/dev-services/consul")

// Applications
// includeBuild("applications/pipeline-engine")
// includeBuild("applications/cli/register-module")

// Modules
// includeBuild("modules/echo")