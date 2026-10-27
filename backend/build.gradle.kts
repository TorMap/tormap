import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tormap"
version = "4.0.0"
java.sourceCompatibility = JavaVersion.VERSION_25

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    kotlin("plugin.allopen") version "2.4.21"
    kotlin("plugin.jpa") version "2.4.21"

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"

    // Build and push docker images
    id("com.google.cloud.tools.jib") version "3.5.4"

    // Test coverage, run with ./gradlew test jacocoTestReport
    jacoco
}

repositories {
    mavenCentral()
}

// Overrides of Spring Boot managed versions to pick up security fixes ahead of a Boot release (reported by Snyk).
// Remove an override once the Boot-managed version is at or above it.
extra["tomcat.version"] = "11.0.26"
extra["jackson-bom.version"] = "3.1.7" // Jackson 3 (tools.jackson)
extra["jackson-2-bom.version"] = "2.22.3" // Jackson 2 (com.fasterxml.jackson), keep in sync with the pins for metrics-lib below

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

    // OpenAPI generation and Swagger UI https://springdoc.org/
    // Only for local development (bootRun) and tests, so it is not in the production image (Jib uses runtimeClasspath).
    // The controllers only need the annotations at compile time; without springdoc they are ignored at runtime.
    // Keep the three versions equal. Inline literals, so CI's OpenSourceMalware check sees version changes.
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-api:3.1.1")
    developmentOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
    testImplementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    // Serialization
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Postgres Database
    implementation("org.postgresql:postgresql:42.7.14")

    // In-memory caching with Caffeine https://github.com/ben-manes/caffeine (version managed by Spring Boot)
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Read .mmdb (MaxMind) DB files for IP lookups https://maxmind.github.io/MaxMind-DB/
    implementation("com.maxmind.geoip2:geoip2:5.2.0")

    // Export metrics to New Relic via OTLP (https://micrometer.io/)
    implementation("org.springframework.boot:spring-boot-starter-micrometer-metrics")
    // Only the OpenTelemetry SDK setup: management.opentelemetry.resource-attributes for the OTLP export. No tracing.
    implementation("org.springframework.boot:spring-boot-opentelemetry")
    implementation("io.micrometer:micrometer-registry-otlp")

    // Reverse DNS lookups with dnsjava
    implementation("dnsjava:dnsjava:3.6.5")

    // Packages required by the vendored metrics-lib (org.torproject.descriptor in java module, see src/main/java/org/torproject/VENDORED.md)
    implementation("commons-codec:commons-codec:1.23.0")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.22")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.22.4")
    implementation("org.slf4j:slf4j-api") // SLF4J 2 (Boot-managed): required by Logback 1.5; compatible with metrics-lib
    implementation("org.tukaani:xz:1.12")

    // Testing JUnit and Kotest (https://kotest.io/)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("io.kotest:kotest-runner-junit5:6.2.6")
    testImplementation("io.kotest:kotest-assertions-core:6.2.6")
    testImplementation("io.kotest:kotest-extensions-spring:6.2.6")

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
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

// Extract the IP lookup databases from the zip archives in ip-lookup/ when the resources are built.
// The archives are only opened at execution time, and the result is a generated resource directory.
val ipLookupResources = layout.buildDirectory.dir("generated/ip-lookup-resources")
interface InjectedArchiveOperations {
    @get:Inject
    val archiveOperations: ArchiveOperations
}
val unpackIpLookupDatabases = tasks.register<Sync>("unpackIpLookupDatabases") {
    // A service instead of Project.zipTree, so the task does not reference the build script (configuration cache)
    val archiveOperations = objects.newInstance<InjectedArchiveOperations>().archiveOperations
    val archives = fileTree("ip-lookup") { include("*.zip") }
    from(archives.elements.map { zips -> zips.map { archiveOperations.zipTree(it) } }) {
        exclude("__MACOSX/**")
        include("**/*.mmdb", "**/COPYRIGHT.txt", "**/LICENSE.txt")
        // Flatten the directories inside the archives
        eachFile { path = name }
        rename(".*GeoLite2-ASN.*\\.mmdb", "autonomous-system.mmdb")
        rename(".*dbip-city-lite.*\\.mmdb", "location.mmdb")
    }
    includeEmptyDirs = false
    into(ipLookupResources.map { it.dir("ip-lookup") })
}
sourceSets.main {
    resources.srcDir(files(ipLookupResources).builtBy(unpackIpLookupDatabases))
}
tasks.processResources {
    // Earlier builds extracted the databases into src/main/resources, which would now collide with the generated ones
    val legacyIpLookupDir = file("src/main/resources/ip-lookup")
    doFirst {
        check(!legacyIpLookupDir.exists()) {
            "Delete $legacyIpLookupDir: the IP lookup databases are now extracted into the build directory."
        }
    }
}

// Configure tests
tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    // Java 25 class files need JaCoCo 0.8.14 or later
    toolVersion = "0.8.15"
}

// Writes build/reports/jacoco/test/html/index.html and an XML report for tools
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    // The vendored metrics-lib (org.torproject) is mostly unused by TorMap, so it would dilute the numbers
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude("org/torproject/**") } })
}

// The local development server uses the dev profile unless SPRING_PROFILES_ACTIVE is set
tasks.bootRun {
    if (providers.environmentVariable("SPRING_PROFILES_ACTIVE").orNull == null) {
        systemProperty("spring.profiles.active", "dev")
    }
}

// Security scans with the Snyk CLI (needs `snyk` on PATH and a prior `snyk auth` or SNYK_TOKEN).
// Extra CLI arguments can be passed with e.g. -PsnykArgs="--json".
fun registerSnykTask(taskName: String, vararg snykCommand: String) = tasks.register<Exec>(taskName) {
    group = "verification"
    workingDir = projectDir
    val extraArgs = providers.gradleProperty("snykArgs").map { it.split(" ").filter(String::isNotBlank) }.orElse(emptyList())
    commandLine(listOf("snyk") + snykCommand + "--severity-threshold=high" + extraArgs.get())
}
registerSnykTask("snykTest", "test")
registerSnykTask("snykCode", "code", "test")

// Mount point for the downloaded descriptors. Jib cannot set file ownership, so the empty directory is world-writable:
// a fresh (named or anonymous) volume mounted there inherits this, and the non-root container user can write to it.
val jibExtraDir = layout.buildDirectory.dir("jib-extra")
val prepareJibExtraDirectories = tasks.register("prepareJibExtraDirectories") {
    val dataDir = jibExtraDir.map { it.dir("tormap-data").asFile }
    outputs.dir(jibExtraDir)
    doLast { dataDir.get().mkdirs() }
}
tasks.matching { it.name.startsWith("jib") }.configureEach { dependsOn(prepareJibExtraDirectories) }

// Configure docker build and push
jib {
    // Like bootJar: the runtime classpath without developmentOnly dependencies (Swagger UI)
    configurationName = "productionRuntimeClasspath"
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
        image = "eclipse-temurin:25-jre@sha256:fcd7fd7b387f94bb2ac461478a7436ad8e349924c374ea8313919624dceae636"
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
