import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.5.21"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion

    // Generate code documentation https://kotlin.github.io/dokka/1.5.0/
    id("org.jetbrains.dokka") version "1.5.0"

    // Spring
    id("org.springframework.boot") version "2.5.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    // Database migration tool https://flywaydb.org/documentation/usage/gradle/
    id("org.flywaydb.flyway") version "7.12.1"
}

group = "org.tormap"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    kotlin("reflect")
    kotlin("stdlib-jdk8")

    // Download and read Tor descriptors (JavaDoc: https://metrics.torproject.org/metrics-lib/index.html)
    implementation(fileTree("lib/metrics-lib-2.17.0"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    val configurationProcessor = "org.springframework.boot:spring-boot-configuration-processor"
    kapt(configurationProcessor)
    kaptTest(configurationProcessor)
    annotationProcessor(configurationProcessor)

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("com.h2database:h2:1.4.199")

    // Run database migration tool on startup
    implementation("org.flywaydb:flyway-core")

    // Provider for geo-ip lookups
    implementation("com.maxmind.geoip2:geoip2:2.15.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kapt {
    annotationProcessor("org.springframework.boot.configurationprocessor.ConfigurationMetadataAnnotationProcessor")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}


tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
