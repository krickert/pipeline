pluginManagement {
    repositories {
        maven { url = uri("https://repo1.maven.org/maven2/") }
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "pipeline-project"

include("grpc-stubs")
include("test-app")
include("libraries:data-util")
include("bom:pipeline-bom")