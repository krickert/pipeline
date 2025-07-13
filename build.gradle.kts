plugins {
    java
    id("io.quarkus") version "3.24.3" apply false
}

allprojects {
    group = "com.pipeline"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}