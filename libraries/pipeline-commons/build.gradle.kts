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

    // Our dependencies
    implementation(project(":libraries:pipeline-api"))
    implementation(project(":grpc-stubs"))

    // Quarkus dependencies
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-jackson")


    // Utilities
    implementation(libs.guava)
    implementation("org.apache.commons:commons-lang3")

    // gRPC and Protobuf
    //implementation("io.grpc:grpc-testing")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.google.protobuf:protobuf-java-util")

    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj)
    testImplementation(libs.jimfs)
    testImplementation("com.github.marschall:memoryfilesystem:2.7.0")
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