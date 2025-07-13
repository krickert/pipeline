pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
    val quarkusPluginVersion: String by settings
    plugins {
        id("io.quarkus.extension") version quarkusPluginVersion
        id("io.quarkus") version quarkusPluginVersion
    }
}
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
includeBuild("../../libraries")
includeBuild("../../protobuf")
rootProject.name = "grpc-stubs-parent"
include(":deployment")
include(":runtime")
project(":deployment").name = "grpc-stubs-deployment"
project(":runtime").name = "grpc-stubs"