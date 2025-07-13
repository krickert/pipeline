plugins {
    `java-library`
    `maven-publish`
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "com.pipeline"
    version = "1.0.0-SNAPSHOT"
    
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "com.pipeline"
                artifactId = project.name
                version = "1.0.0-SNAPSHOT"
                from(components["java"])
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.pipeline"
            artifactId = rootProject.name
            version = "1.0.0-SNAPSHOT"
            from(components["java"])
        }
    }
}
group = "com.pipeline"
version = "1.0.0-SNAPSHOT"