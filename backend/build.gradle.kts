import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tormap"
version = "2.3.1"
java.sourceCompatibility = JavaVersion.VERSION_11

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("kapt") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.allopen") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "2.7.18"
    id("io.spring.dependency-management") version "1.1.4"

    // Build and push docker images
    id("com.google.cloud.tools.jib") version "3.4.2"
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
    implementation("org.springdoc:springdoc-openapi-ui:1.8.0")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.8.0")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

    // Postgres Database
    implementation("org.postgresql:postgresql:42.7.4")

    // Caching with Ehcache https://www.ehcache.org/
    implementation("org.ehcache:ehcache:3.10.8")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.flywaydb:flyway-core:8.5.13")

    // Read .mmdb (MaxMind) DB files for IP lookups https://maxmind.github.io/MaxMind-DB/
    implementation("com.maxmind.geoip2:geoip2:4.2.1")

    // Collect metrics
    implementation("com.newrelic.telemetry:micrometer-registry-new-relic:0.10.0")

    // Packages required by metrics-lib (org.torproject.descriptor in java module) (JavaDoc: https://metrics.torproject.org/metrics-lib/index.html)
    implementation("commons-codec:commons-codec:1.10")
    implementation("org.apache.commons:commons-compress:1.13")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.12.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.4")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.tukaani:xz:1.6")

    // Testing JUnit and Kotest (https://kotest.io/)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
    // Testcontainers to provide Postgres DB (https://testcontainers.org/)
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    // Mocking with Mockk (https://mockk.io/)
    testImplementation("io.mockk:mockk:1.13.13")
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
