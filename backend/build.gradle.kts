import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.6.0"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion

    // Generate code documentation https://kotlin.github.io/dokka/1.5.0/
    id("org.jetbrains.dokka") version "1.5.0"

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "2.5.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    // Database migration tool https://flywaydb.org/documentation/usage/gradle/
    id("org.flywaydb.flyway") version "8.0.3"
}

group = "org.tormap"
version = "1.1.2"
java.sourceCompatibility = JavaVersion.VERSION_11

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
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // OpenAPI generation and Swagger UI https://springdoc.org/
    implementation("org.springdoc:springdoc-openapi-ui:1.5.11")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.5.11")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")

    // Latest stable H2 database driver https://www.h2database.com/
    runtimeOnly("com.h2database:h2:1.4.200")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.flywaydb:flyway-core")

    // IP to geo location lookups
    implementation("com.maxmind.geoip2:geoip2:2.16.1")

    // Packages required by metrics-lib (org.torproject.descriptor in java module) (JavaDoc: https://metrics.torproject.org/metrics-lib/index.html)
    implementation("commons-codec:commons-codec:1.10")
    implementation("org.apache.commons:commons-compress:1.13")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.12.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.4")
    implementation("org.slf4j:slf4j-api:1.7.22")
    implementation("org.tukaani:xz:1.6")
}

// Allow JPA annotations for Kotlin classes
allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

// Connect migration tool to DB
flyway {
    url = "jdbc:h2:./resources/database/tormap;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=5;LOCK_TIMEOUT=30000"
    user = "sa"
}

// Build image for docker https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/#build-image
tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
    imageName = "juliushenke/tormap"

    val relativePathIpLookup = "/ip-lookup/"
    bindings = listOf("${rootProject.projectDir.absolutePath}$relativePathIpLookup:/workspace$relativePathIpLookup")
}

// Configure KotlinDoc generation
tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))
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
