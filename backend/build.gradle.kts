import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tormap"
version = "3.0.1"
java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("kapt") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    kotlin("plugin.allopen") version "2.4.20"
    kotlin("plugin.jpa") version "2.4.20"

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"

    // Build and push docker images
    id("com.google.cloud.tools.jib") version "3.5.4"
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    kotlin("reflect")

    // Spring Boot https://spring.io/projects/spring-boot
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // OpenAPI generation and Swagger UI https://springdoc.org/
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    // Serialization
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Postgres Database
    implementation("org.postgresql:postgresql:42.7.13")

    // Caching with Ehcache https://www.ehcache.org/
    implementation("org.ehcache:ehcache:3.12.0:jakarta")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Read .mmdb (MaxMind) DB files for IP lookups https://maxmind.github.io/MaxMind-DB/
    implementation("com.maxmind.geoip2:geoip2:5.2.0")

    // Export metrics to New Relic via OTLP (https://micrometer.io/)
    implementation("org.springframework.boot:spring-boot-starter-micrometer-metrics")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.micrometer:micrometer-registry-otlp")

    // Reverse DNS lookups with dnsjava
    implementation("dnsjava:dnsjava:3.6.5")

    // Packages required by metrics-lib (org.torproject.descriptor in java module) (JavaDoc: https://metrics.torproject.org/metrics-lib/index.html)
    implementation("commons-codec:commons-codec:1.22.1")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.22")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.22.2")
    implementation("org.slf4j:slf4j-api") // SLF4J 2 (Boot-managed): required by Logback 1.5; compatible with metrics-lib
    implementation("org.tukaani:xz:1.12")

    // Testing JUnit and Kotest (https://kotest.io/)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("io.kotest:kotest-runner-junit5:6.2.5")
    testImplementation("io.kotest:kotest-assertions-core:6.2.5")
    testImplementation("io.kotest:kotest-extensions-spring:6.2.5")

    // Testcontainers to provide Postgres DB (https://testcontainers.org/)
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")

    // Mocking with Mockk (https://mockk.io/)
    testImplementation("io.mockk:mockk:1.14.11")
}

// Allow JPA annotations for Kotlin classes
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

// Allow version info to be injected
springBoot {
    buildInfo()
}

// Compile options for JVM build
tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.register<Copy>("unpackFiles") {
    val tree = fileTree("ip-lookup")
    tree.include("*.zip")
    fileTree("ip-lookup").forEach { file ->
        from(zipTree(file).files)
        into("src/main/resources/ip-lookup")
        rename(".*GeoLite2-ASN.*mmdb", "autonomous-system.mmdb")
        rename(".*dbip-city-lite.*mmdb", "location.mmdb")
    }
}
tasks.assemble {
    dependsOn("unpackFiles")
}
tasks.processResources {
    dependsOn("unpackFiles")
}

// Configure tests
tasks.withType<Test> {
    useJUnitPlatform()
}

// Mount point for the downloaded descriptors. Jib cannot set file ownership, so the empty directory is world-writable:
// a fresh (named or anonymous) volume mounted there inherits this, and the non-root container user can write to it.
val jibExtraDir = layout.buildDirectory.dir("jib-extra")
val prepareJibExtraDirectories by tasks.registering {
    val dataDir = jibExtraDir.map { it.dir("tormap-data").asFile }
    outputs.dir(jibExtraDir)
    doLast { dataDir.get().mkdirs() }
}
tasks.matching { it.name.startsWith("jib") }.configureEach { dependsOn(prepareJibExtraDirectories) }

// Configure docker build and push
jib {
    to {
        image = "tormap/backend"
        auth {
            username = System.getenv("DOCKERHUB_USERNAME")
            password = System.getenv("DOCKERHUB_TOKEN")
        }
        tags = setOf(version.toString(), version.toString().substringBefore('.'))
    }
    container {
        // Run as non-root. Logs go to /tmp since the root filesystem is not writable for this user.
        user = "1000:1000"
        workingDirectory = "/"
        environment = mapOf("LOG_DIR" to "/tmp/logs")
        volumes = listOf("/tormap-data")
    }
    extraDirectories {
        paths {
            path {
                setFrom(jibExtraDir)
                into = "/"
            }
        }
        permissions = mapOf("/tormap-data" to "777")
    }
    from {
        // Pinned by digest for reproducible builds; Renovate keeps the digest current
        image = "eclipse-temurin:21-jre@sha256:cff19e6215689161eb6162c11b86b0c60ddf802164f2eaf48d570f8fb79a36c5"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
}
