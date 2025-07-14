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
    implementation(project(":libraries:pipeline-api"))
    implementation(project(":libraries:pipeline-commons"))
    implementation(project(":grpc-stubs"))
    implementation("io.quarkus:quarkus-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("jakarta.annotation:jakarta.annotation-api")
    testImplementation(libs.assertj)
    testImplementation("io.quarkus:quarkus-junit5")
}

tasks.named("compileJava") {
    dependsOn(tasks.named("compileQuarkusGeneratedSourcesJava"))
}

