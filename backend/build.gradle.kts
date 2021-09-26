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

    // Spring https://spring.io/projects/spring-boot
    id("org.springframework.boot") version "2.5.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    // Database migration tool https://flywaydb.org/documentation/usage/gradle/
    id("org.flywaydb.flyway") version "7.12.1"
}

group = "org.tormap"
version = "1.1.0"
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
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // OpenAPI generation and Swagger UI https://springdoc.org/
    implementation("org.springdoc:springdoc-openapi-ui:1.3.+")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.3.+")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Latest stable H2 database driver https://www.h2database.com/
    runtimeOnly("com.h2database:h2:1.4.199")

    // Run Flyway DB migration tool on startup https://flywaydb.org/
    implementation("org.flywaydb:flyway-core")

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
    url = "jdbc:h2:./database/tormap;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=5;LOCK_TIMEOUT=30000"
    user = "sa"
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
    imageName = "juliushenke/tormap"

    val relativePathIp2LocationDB = "/ip2location/"
    bindings = listOf("${rootProject.projectDir.absolutePath}$relativePathIp2LocationDB:/workspace$relativePathIp2LocationDB")
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
