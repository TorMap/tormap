import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tormap"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11

plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("kapt") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
    kotlin("plugin.allopen") version "1.7.22"
    kotlin("plugin.jpa") version "1.7.22"

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "2.7.6"
    id("io.spring.dependency-management") version "1.1.0"

    // Build and push docker images
    id("com.google.cloud.tools.jib") version "3.3.1"
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
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // OpenAPI generation and Swagger UI https://springdoc.org/
    implementation("org.springdoc:springdoc-openapi-ui:1.6.13")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.6.13")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.1")

    // Postgres Database
    implementation("org.postgresql:postgresql:42.5.1")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.flywaydb:flyway-core:8.5.13")

    // Read .mmdb (MaxMind) DB files for IP lookups https://maxmind.github.io/MaxMind-DB/
    implementation("com.maxmind.geoip2:geoip2:3.0.2")

    // Anaylz user agent https://yauaa.basjes.nl/
    implementation("nl.basjes.parse.useragent:yauaa:7.9.0")

    // Packages required by metrics-lib (org.torproject.descriptor in java module) (JavaDoc: https://metrics.torproject.org/metrics-lib/index.html)
    implementation("commons-codec:commons-codec:1.10")
    implementation("org.apache.commons:commons-compress:1.13")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.12.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.4")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.tukaani:xz:1.6")

    // Testing with Kotest (https://kotest.io/) and Testcontainers (https://testcontainers.org/)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.kotest:kotest-runner-junit5:5.5.4")
    testImplementation("io.kotest:kotest-assertions-core:5.5.4")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2")
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation("org.testcontainers:postgresql:1.17.6")
}

// Fix version requirement from Kotest
extra["kotlin-coroutines.version"] = "1.6.0"

// Allow JPA annotations for Kotlin classes
allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

// Compile options for JVM build
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

// Configure tests
tasks.withType<Test> {
    useJUnitPlatform()
}

// Configure docker build and push
jib {
    to {
        image = "tormap/backend"
        tags = setOf(version.toString(), version.toString().substringBefore('.'))
    }
    from {
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
