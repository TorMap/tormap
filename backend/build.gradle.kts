import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tormap"
version = "3.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("kapt") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    kotlin("plugin.allopen") version "2.4.20"
    kotlin("plugin.jpa") version "2.4.20"

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "2.7.18"
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
    kotlin("stdlib-jdk8")

    // Spring Boot https://spring.io/projects/spring-boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // OpenAPI generation and Swagger UI https://springdoc.org/
    implementation("org.springdoc:springdoc-openapi-ui:1.8.0")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.8.0")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.2")

    // Postgres Database
    implementation("org.postgresql:postgresql:42.7.13")

    // Caching with Ehcache https://www.ehcache.org/
    implementation("org.ehcache:ehcache:3.11.1")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.flywaydb:flyway-core:8.5.13")

    // Read .mmdb (MaxMind) DB files for IP lookups https://maxmind.github.io/MaxMind-DB/
    implementation("com.maxmind.geoip2:geoip2:4.4.0")

    // Collect metrics
    implementation("com.newrelic.telemetry:micrometer-registry-new-relic:0.10.0")

    // Reverse DNS lookups with dnsjava
    implementation("dnsjava:dnsjava:3.6.5")

    // Packages required by metrics-lib (org.torproject.descriptor in java module) (JavaDoc: https://metrics.torproject.org/metrics-lib/index.html)
    implementation("commons-codec:commons-codec:1.22.1")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.22")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.22.2")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.tukaani:xz:1.12")

    // Testing JUnit and Kotest (https://kotest.io/)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.kotest:kotest-runner-junit5:6.2.5")
    testImplementation("io.kotest:kotest-assertions-core:6.2.5")
    testImplementation("io.kotest:kotest-extensions-spring:6.2.5")

    // Testcontainers to provide Postgres DB (https://testcontainers.org/)
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")

    // Mocking with Mockk (https://mockk.io/)
    testImplementation("io.mockk:mockk:1.14.11")
}

// Interim security patches: Spring Boot 2.7 is end-of-life, so override managed versions with the latest releases of the
// same lines. Remove these when migrating to Spring Boot 3+.
extra["tomcat.version"] = "9.0.122"
extra["spring-framework.version"] = "5.3.39"
extra["spring-security.version"] = "5.7.14"
extra["logback.version"] = "1.2.13"

// Allow JPA annotations for Kotlin classes
allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

// Allow version info to be injected
springBoot {
    buildInfo()
}

// Compile options for JVM build
tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
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
        image = "eclipse-temurin:17-jre@sha256:207ecae0b2b104dfc6dfa763d1e9cd2041cb344800a180e24cb1a28ed533ff90"
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
