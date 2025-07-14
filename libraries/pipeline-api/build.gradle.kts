plugins {
    `java-library`
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

dependencies {
    // Quarkus BOM (applies to all configurations)
    implementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    testImplementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    implementation(project(":grpc-stubs"))
    implementation("io.quarkus:quarkus-smallrye-stork")

    // Quarkus dependencies
    implementation("io.quarkus:quarkus-jackson")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-mutiny")

    // Swagger annotations - included with quarkus-smallrye-openapi

    // Our protobuf definitions
    implementation(project(":grpc-stubs"))

    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj)
    testImplementation(libs.jimfs)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.named("compileJava") {
    dependsOn(tasks.named("compileQuarkusGeneratedSourcesJava"))
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
