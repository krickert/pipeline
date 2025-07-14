// Parent build file for Consul DevServices Quarkus Extension
plugins {
    java
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "com.pipeline"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
}