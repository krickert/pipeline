plugins {
    id("io.quarkus") version "3.24.3" apply false
}

allprojects {
    group = "com.pipeline"
    version = "1.0.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
}