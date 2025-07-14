plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

group = "com.pipeline"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(platform("io.quarkus.platform:quarkus-bom:3.24.3"))
}