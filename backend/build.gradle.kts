import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.IOException
import java.net.URI
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

// Locks the plugin classpath too (buildscript-gradle.lockfile), see dependencyLocking below
buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
    // Patched versions of libraries the Jib and Spring Boot plugins pull in, to keep known advisories out of the build.
    // Drop each one once the plugins depend on a fixed version themselves.
    dependencies {
        classpath(platform("com.fasterxml.jackson:jackson-bom:2.23.0"))
        constraints {
            classpath("com.google.guava:guava:33.8.0-jre")
            classpath("org.apache.commons:commons-lang3:3.22.0")
        }
    }
}

group = "org.tormap"
version = "4.3.2"
java.sourceCompatibility = JavaVersion.VERSION_25

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    kotlin("plugin.allopen") version "2.4.21"
    kotlin("plugin.jpa") version "2.4.21"

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "4.2.0"
    id("io.spring.dependency-management") version "1.1.7"

    // Build and push docker images
    id("com.google.cloud.tools.jib") version "3.5.4"

    // Test coverage, run with ./gradlew test jacocoTestReport
    jacoco
}

// Dependency locking: every resolved version, including transitive and BOM-managed ones, is pinned in gradle.lockfile
// (the plugins in buildscript-gradle.lockfile), so builds only change when the lockfile does.
// After changing a dependency, update the lockfiles with: ./gradlew resolveAndLockAll --write-locks
dependencyLocking {
    lockAllConfigurations()
}

tasks.register("resolveAndLockAll") {
    description = "Resolves all configurations, to write the lockfile with --write-locks"
    notCompatibleWithConfigurationCache("Filters configurations at execution time")
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) { "Run with --write-locks" }
    }
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}

// Overrides of Spring Boot managed versions to pick up security fixes ahead of a Boot release (reported by Snyk).
// Remove an override once the Boot-managed version is at or above it.
// Jackson 2 (com.fasterxml.jackson), keep in sync with the pins for metrics-lib below
extra["jackson-2-bom.version"] = "2.23.0"
// Tomcat (Spring Boot 4.2.0 manages 11.0.27)
extra["tomcat.version"] = "11.0.29"

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
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-api:3.2.1")
    developmentOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.1")
    testImplementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.1")

    // Serialization
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Postgres Database
    implementation("org.postgresql:postgresql:42.7.15")

    // In-memory caching with Caffeine https://github.com/ben-manes/caffeine (version managed by Spring Boot)
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Read .mmdb (MaxMind DB format) files for IP lookups https://maxmind.github.io/MaxMind-DB/
    implementation("com.maxmind.geoip2:geoip2:5.3.0")

    // Export metrics to New Relic via OTLP (https://micrometer.io/)
    implementation("org.springframework.boot:spring-boot-starter-micrometer-metrics")
    // Only the OpenTelemetry SDK setup: management.opentelemetry.resource-attributes for the OTLP export. No tracing.
    implementation("org.springframework.boot:spring-boot-opentelemetry")
    implementation("io.micrometer:micrometer-registry-otlp")

    // Reverse DNS lookups with dnsjava
    implementation("dnsjava:dnsjava:3.7.1")

    // Packages required by the vendored metrics-lib (org.torproject.descriptor in java module, see src/main/java/org/torproject/VENDORED.md)
    implementation("commons-codec:commons-codec:1.23.0")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.23")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.23.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.23.0")
    // SLF4J 2 (Boot-managed): required by Logback 1.5; compatible with metrics-lib
    implementation("org.slf4j:slf4j-api")
    implementation("org.tukaani:xz:1.12")

    // Testing JUnit and Kotest (https://kotest.io/)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("io.kotest:kotest-runner-junit5:6.2.7")
    testImplementation("io.kotest:kotest-assertions-core:6.2.7")
    testImplementation("io.kotest:kotest-extensions-spring:6.2.7")

    // Testcontainers to provide Postgres DB (https://testcontainers.org/)
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")

    // Mocking with Mockk (https://mockk.io/)
    testImplementation("io.mockk:mockk:1.14.12")
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

// The IP lookup databases (DB-IP Lite, CC BY 4.0) are not stored in this repository. They are published monthly as
// immutable releases of https://github.com/TorMap/ip-lookup-data and downloaded when the resources are built.
// The release is pinned by ipLookupDataVersion in gradle.properties (updated by Renovate) and checked against its
// SHA256SUMS. Override the source with -PipLookupDataUrl=<base URL> (e.g. a file: URL to build offline).
abstract class DownloadIpLookupDatabases : DefaultTask() {
    @get:Input
    abstract val releaseVersion: Property<String>

    @get:Input
    abstract val baseUrl: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val version = releaseVersion.get()
        require(Regex("""\d{4}-\d{2}""").matches(version)) { "Invalid ipLookupDataVersion '$version'" }
        val releaseUrl = "${baseUrl.get().trimEnd('/')}/$version"
        val checksums =
            fetch("$releaseUrl/SHA256SUMS")
                .decodeToString()
                .lines()
                .filter { it.isNotBlank() }
                .associate { line -> line.trim().split(Regex("""\s+\*?""")).let { it[1] to it[0].lowercase() } }
        val outputDir = outputDirectory.get().asFile
        outputDir.deleteRecursively()
        val targetDir = outputDir.resolve("ip-lookup").apply { mkdirs() }
        mapOf(
            "dbip-city-lite.mmdb.gz" to "location.mmdb",
            "dbip-asn-lite.mmdb.gz" to "autonomous-system.mmdb",
        ).forEach { (asset, target) ->
            val bytes = fetch("$releaseUrl/$asset")
            val expected = checksums[asset] ?: throw GradleException("$asset is missing in $releaseUrl/SHA256SUMS")
            val actual =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it) }
            if (actual != expected) throw GradleException("SHA-256 mismatch for $releaseUrl/$asset")
            GZIPInputStream(bytes.inputStream()).use { input ->
                targetDir.resolve(target).outputStream().use { input.copyTo(it) }
            }
        }
    }

    // URLConnection follows the redirect to GitHub's asset storage and uses the JVM proxy settings
    private fun fetch(url: String): ByteArray = try {
        val connection = URI(url).toURL().openConnection()
        connection.connectTimeout = 30_000
        connection.readTimeout = 120_000
        connection.getInputStream().use { it.readBytes() }
    } catch (exception: IOException) {
        throw GradleException("Could not download $url (IP lookup databases need network access once): $exception")
    }
}
val ipLookupResources = layout.buildDirectory.dir("generated/ip-lookup-resources")
val downloadIpLookupDatabases =
    tasks.register<DownloadIpLookupDatabases>("downloadIpLookupDatabases") {
        releaseVersion = providers.gradleProperty("ipLookupDataVersion")
        baseUrl =
            providers
                .gradleProperty("ipLookupDataUrl")
                .orElse("https://github.com/TorMap/ip-lookup-data/releases/download")
        outputDirectory = ipLookupResources
    }
sourceSets.main {
    resources.srcDir(downloadIpLookupDatabases)
}
tasks.processResources {
    // The Lite map draws the same country borders and colors as the frontend, so the files are shared instead of copied
    from(files("../frontend/src/resources/world.geo.json", "../frontend/src/resources/country-meta.json")) {
        into("lite")
    }
    // Earlier builds extracted the databases into src/main/resources, which would now collide with the generated ones
    val legacyIpLookupDir = file("src/main/resources/ip-lookup")
    doFirst {
        check(!legacyIpLookupDir.exists()) {
            "Delete $legacyIpLookupDir: the IP lookup databases are now downloaded into the build directory."
        }
    }
}

// Fuzz targets for Jazzer in src/fuzz, built and run on a schedule by ClusterFuzzLite (see .clusterfuzzlite/).
// The Jazzer API jar comes with the fuzzing image (-PjazzerApi=<path>), so it is not a project dependency.
val fuzz: SourceSet by sourceSets.creating {
    // Only the classes: the resources include the IP lookup databases, which the fuzz targets don't need
    compileClasspath += sourceSets.main.get().output.classesDirs
    runtimeClasspath += sourceSets.main.get().output.classesDirs
}
configurations[fuzz.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[fuzz.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())
providers.gradleProperty("jazzerApi").orNull?.let { dependencies.add(fuzz.compileOnlyConfigurationName, files(it)) }

val fuzzJar = tasks.register<Jar>("fuzzJar") {
    archiveFileName = "tormap-fuzz.jar"
    from(fuzz.output, sourceSets.main.get().output.classesDirs)
}

tasks.register<Sync>("installFuzzTargets") {
    description = "Copies the fuzz targets with their runtime classpath to build/fuzz, see .clusterfuzzlite/build.sh"
    doFirst { require(providers.gradleProperty("jazzerApi").isPresent) { "Run with -PjazzerApi=<jazzer API jar>" } }
    from(fuzzJar, configurations[fuzz.runtimeClasspathConfigurationName])
    into(layout.buildDirectory.dir("fuzz"))
}

// Configure tests
tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    // Java 25 class files need JaCoCo 0.8.14 or later
    toolVersion = "0.8.16"
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

// Dependency scan (SCA) with the Snyk CLI (needs `snyk` on PATH and a prior `snyk auth` or SNYK_TOKEN).
// Extra CLI arguments can be passed with e.g. -PsnykArgs="--json".
tasks.register<Exec>("snykTest") {
    group = "verification"
    workingDir = projectDir
    val extraArgs =
        providers
            .gradleProperty("snykArgs")
            .map {
                it.split(" ").filter(String::isNotBlank)
            }.orElse(emptyList())
    commandLine(listOf("snyk", "test", "--severity-threshold=high") + extraArgs.get())
}

// Mount point for the downloaded descriptors. Jib cannot set file ownership, so the empty directory is world-writable:
// a fresh (named or anonymous) volume mounted there inherits this, and the non-root container user can write to it.
val jibExtraDir = layout.buildDirectory.dir("jib-extra")
val prepareJibExtraDirectories =
    tasks.register("prepareJibExtraDirectories") {
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
        // Temurin JRE on Alpine: the smallest official Temurin image, for amd64 and arm64.
        // Pinned by digest for reproducible builds; Renovate keeps the digest current (see renovate.json).
        image = "eclipse-temurin:25-jre-alpine@sha256:69c2a26e9894016e274c880fa542956d005b3755f84db765145185e87405bd89"
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

// Kotlin code style (https://pinterest.github.io/ktlint/), part of `check`. ./gradlew ktlintFormat fixes most findings.
val ktlint: Configuration by configurations.creating {
    // Spring's dependency management would raise ktlint's embedded Kotlin compiler to the project's Kotlin version,
    // which ktlint can't run with. Keep the versions ktlint asks for.
    // Except logback: ktlint still asks for 1.3.x, which has known advisories, so it gets the patched line.
    resolutionStrategy.eachDependency {
        if (requested.group == "ch.qos.logback") {
            useVersion("1.6.6")
        } else {
            requested.version?.let { useVersion(it) }
        }
    }
}

dependencies {
    ktlint("com.pinterest.ktlint:ktlint-cli:1.8.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintSources = listOf("src/**/*.kt", "*.kts", "!**/build/**")

val ktlintCheck = tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks the Kotlin code style with ktlint"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    // ktlint's embedded Kotlin compiler still uses sun.misc.Unsafe
    jvmArgs("--sun-misc-unsafe-memory-access=allow")
    args(ktlintSources)
}

tasks.check {
    dependsOn(ktlintCheck)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fixes the Kotlin code style with ktlint where it can"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    args(listOf("--format") + ktlintSources)
}
