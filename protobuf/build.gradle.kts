plugins {
    java
    `maven-publish`
}

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

description = "Protocol Buffer definitions for Pipeline project"

// This is a resource-only JAR
sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
}

// No Java code, just resources
tasks.compileJava {
    enabled = false
}

// Ensure proto files are included in the JAR
tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Pipeline Protocol Buffers")
                description.set("Protocol Buffer definitions for the Pipeline project")
            }
        }
    }
}