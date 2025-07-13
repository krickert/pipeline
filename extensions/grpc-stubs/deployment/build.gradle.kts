plugins {
    java
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(platform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    annotationProcessor("io.quarkus:quarkus-extension-processor:${quarkusPlatformVersion}")

    api(project(":grpc-stubs")) // why: https://quarkus.io/guides/building-my-first-extension
    implementation("io.quarkus:quarkus-core-deployment")
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-grpc-deployment")
    implementation("io.quarkus:quarkus-cache-deployment")

    testImplementation("io.quarkus:quarkus-junit5-internal")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.quarkus:quarkus-resteasy-deployment")
}

java {
    // withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}