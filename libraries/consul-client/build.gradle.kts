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

    // Quarkus extensions
    implementation("io.quarkiverse.config:quarkus-config-consul")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-cache")

    // Stork for service discovery
    implementation("io.smallrye.stork:stork-service-discovery-consul")

    // Vertx Consul client
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")

    // Migrated project dependencies
    implementation(project(":libraries:pipeline-api"))
    implementation(project(":libraries:pipeline-commons"))
    implementation(project(":grpc-stubs"))
    implementation(project(":libraries:validators"))

    // Test dependencies
    testImplementation(project(":libraries:testing-commons"))
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.awaitility:awaitility")
    // https://mvnrepository.com/artifact/com.networknt/json-schema-validator
    implementation("com.networknt:json-schema-validator:1.5.8")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")



}
