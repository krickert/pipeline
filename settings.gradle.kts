pluginManagement {
    repositories {
        maven { url = uri("https://repo1.maven.org/maven2/") }
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "pipeline-project"

include("bom:pipeline-bom")
include("grpc-stubs")
include("integration-test-app")
include("libraries:data-util")
include("libraries:pipeline-api")
include("libraries:pipeline-commons")
include("libraries:validators")
include("libraries:testing-commons")
include("libraries:testing-server-util")
include("libraries:consul-client")

// Extensions
include("extensions:consul-devservices")
include("extensions:consul-devservices:runtime")
include("extensions:consul-devservices:deployment")
include("extensions:consul-devservices:integration-tests")
