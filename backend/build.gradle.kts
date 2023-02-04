import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tormap"
version = "1.1.0"

plugins {
    val kotlin = "1.8.10"
    kotlin("jvm") version kotlin
    kotlin("plugin.spring") version kotlin

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.graalvm.buildtools.native") version "0.9.19"

    // flyway
    id("org.flywaydb.flyway") version "9.11.0"

    // Build and push docker images
    id("com.google.cloud.tools.jib") version "3.3.1"

    // openapi docu
    id("org.springdoc.openapi-gradle-plugin") version "1.6.0"
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    kotlin("reflect")
    kotlin("stdlib")

    // Spring Boot https://spring.io/projects/spring-boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // OpenAPI generation and Swagger UI https://springdoc.org/
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Postgres Database
    runtimeOnly("org.postgresql:postgresql:42.5.1")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    runtimeOnly("org.flywaydb:flyway-core")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    // Read .mmdb (MaxMind) DB files for IP lookups https://maxmind.github.io/MaxMind-DB/
    implementation("com.maxmind.geoip2:geoip2:4.0.0")

    // Analyze user agent https://yauaa.basjes.nl/
    implementation("com.github.ua-parser:uap-java:1.5.3")

    // Packages required by metrics-lib (org.torproject.descriptor in java module)
    // (JavaDoc: https://metrics.torproject.org/metrics-lib/index.html)
    implementation("commons-codec:commons-codec:1.10")
    implementation("org.apache.commons:commons-compress:1.13")
    implementation("org.tukaani:xz:1.6")

    // Testing with Kotest (https://kotest.io/) and Testcontainers (https://testcontainers.org/)
    // Spring
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // mockk
    testImplementation("io.mockk:mockk:1.13.3")
    testImplementation("com.ninja-squad:springmockk:4.0.0")
    // kotest
    val kotest = "5.5.4"
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotest")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2")
    // Test Containers
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.17.6"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
}

tasks {
    bootJar {
        archiveFileName.set("tormap.jar")
    }
}

tasks.register("bootRunWithNativeImageAgent") {
    group = "application"
    description = "Runs bootRun with GraalVM tracing agent enabled"
    doFirst {
        tasks.bootRun {
            jvmArgs = listOf("-agentlib:native-image-agent=config-output-dir=native-image")
        }
    }
    finalizedBy("bootRun")
}

graalvmNative {
    agent {
        defaultMode.set("standard")
    }
    toolchainDetection.set(false)
    binaries {
        all {
            resources.autodetect()
        }
        named("main") {
            buildArgs("-H:+StaticExecutableWithDynamicLibC")
        }
    }
    metadataRepository {
        enabled.set(true)
    }
}

// Compile options for JVM build
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        javaParameters = true
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-Xemit-jvm-type-annotations",
            "-Xjvm-default=all",
            "-Xcontext-receivers"
        )
        jvmTarget = "${JavaVersion.VERSION_17}"
    }
}

// Configure tests
tasks.withType<Test> {
    useJUnitPlatform()
}
