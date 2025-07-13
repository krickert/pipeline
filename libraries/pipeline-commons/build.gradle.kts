plugins {
    java
    id("io.quarkus")
    `maven-publish`
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
    // Quarkus BOM
    implementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    
    // Our dependencies
    implementation("com.pipeline:pipeline-api:1.0.0-SNAPSHOT")
    implementation("com.pipeline:protobuf:1.0.0-SNAPSHOT")
    
    // Quarkus dependencies
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-jackson")
    
    // Docker client
    implementation("com.github.docker-java:docker-java-core")
    implementation("com.github.docker-java:docker-java-transport-httpclient5")
    
    // Utilities
    implementation("com.google.guava:guava")
    implementation("org.apache.commons:commons-lang3")
    
    // gRPC and Protobuf
    implementation("io.grpc:grpc-testing")
    implementation("com.google.protobuf:protobuf-java")
    implementation("com.google.protobuf:protobuf-java-util")
    
    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.google.jimfs:jimfs:1.3.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Pipeline Commons")
                description.set("Common utilities for the Pipeline project")
            }
        }
    }
}