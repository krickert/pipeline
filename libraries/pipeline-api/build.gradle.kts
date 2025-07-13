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
    implementation("io.quarkus:quarkus-smallrye-stork")
    // Quarkus BOM
    implementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    
    // Quarkus dependencies
    implementation("io.quarkus:quarkus-jackson")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-mutiny")
    
    // Swagger annotations - included with quarkus-smallrye-openapi
    
    // Our protobuf definitions
    implementation("com.pipeline:protobuf:1.0.0-SNAPSHOT")
    
    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
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
                name.set("Pipeline API")
                description.set("Core API interfaces and models for the Pipeline project")
            }
        }
    }
}