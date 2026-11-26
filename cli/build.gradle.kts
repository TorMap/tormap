import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Locks the plugin classpath too (buildscript-gradle.lockfile), see dependencyLocking below
buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

group = "org.tormap"
// Shared with backend/build.gradle.kts and frontend/package.json: bump all three before tagging a release
version = "4.3.1"
java.sourceCompatibility = JavaVersion.VERSION_25

plugins {
    kotlin("jvm") version "2.4.21"
    application
}

repositories {
    mavenCentral()
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

dependencies {
    // Command line parsing without the Mordant terminal library https://ajalt.github.io/clikt/
    implementation("com.github.ajalt.clikt:clikt-core:5.2.1")

    // JSON parsing without reflection, so native images need no extra configuration https://github.com/Kotlin/kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")

    // Testing with Kotest (https://kotest.io/), same version as the backend
    testImplementation("io.kotest:kotest-runner-junit5:6.2.7")
    testImplementation("io.kotest:kotest-assertions-core:6.2.7")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

application {
    mainClass.set("org.tormap.cli.MainKt")
    applicationName = "tormap"
}

// The version is compiled in, so `tormap --version` and the User-Agent header work in native images too
val generateVersion by tasks.registering {
    val outputDirectory = layout.buildDirectory.dir("generated/version")
    val projectVersion = project.version.toString()
    inputs.property("version", projectVersion)
    outputs.dir(outputDirectory)
    doLast {
        val file = outputDirectory.get().file("org/tormap/cli/Version.kt").asFile
        file.parentFile.mkdirs()
        file.writeText("package org.tormap.cli\n\ninternal const val VERSION = \"$projectVersion\"\n")
    }
}
kotlin.sourceSets.main { kotlin.srcDir(generateVersion) }

tasks.test {
    useJUnitPlatform()
    // The contract tests read the API v1 spec from the repository root
    systemProperty("tormap.apiSpec", rootDir.resolve("../api/openapi-v1.json").absolutePath)
    inputs.file("../api/openapi-v1.json")
}

/*
 * Native binary with GraalVM native-image, without the GraalVM Gradle plugin: `./gradlew nativeCompile` with
 * GRAALVM_HOME (or JAVA_HOME) pointing to a GraalVM 25. Writes build/native/tormap (tormap.exe on Windows).
 * The CLI uses no reflection or dynamic proxies, so native-image needs no reachability metadata. Its only resource,
 * the service provider in META-INF/services (the name resolver of LocalDns.kt), is included from the classpath.
 */
val nativeCompile by tasks.registering(Exec::class) {
    group = "build"
    description = "Builds a native binary with GraalVM native-image"
    val jar = tasks.jar
    val runtimeClasspath = configurations.runtimeClasspath
    dependsOn(jar)
    inputs.files(jar, runtimeClasspath)
    val outputDirectory = layout.buildDirectory.dir("native")
    outputs.dir(outputDirectory)

    val windows = System.getProperty("os.name").startsWith("Windows")
    val linux = System.getProperty("os.name") == "Linux"
    val graalHome = providers.environmentVariable("GRAALVM_HOME").orElse(providers.environmentVariable("JAVA_HOME"))
    val nativeImage = graalHome.map { "$it/bin/native-image" + if (windows) ".cmd" else "" }
    // Extra native-image options, e.g. -PnativeImageOptions=--verbose
    val extraOptions = providers.gradleProperty("nativeImageOptions").map { it.split(" ").filter(String::isNotBlank) }.orElse(emptyList())
    inputs.property("nativeImageOptions", extraOptions)

    doFirst {
        outputDirectory.get().asFile.mkdirs()
        val classpath = (listOf(jar.get().archiveFile.get().asFile) + runtimeClasspath.get().files)
            .joinToString(File.pathSeparator) { it.absolutePath }
        commandLine(
            listOf(
                nativeImage.get(),
                "--no-fallback",
                // Only http and https are used; a URL with another protocol fails instead of loading a handler
                "--enable-url-protocols=http,https",
                // Smaller binary (23 instead of 35 MB), the CLI spends its time waiting for the network anyway
                "-Os",
                // Runs on every CPU of the architecture, not only on recent ones (the default is x86-64-v3)
                "-march=compatibility",
            ) + (
                // Links zlib in, so the binary only needs glibc (from the build machine's version on)
                if (linux) listOf("--static-nolibc") else emptyList()
            ) + listOf(
                "-o", outputDirectory.get().file("tormap").asFile.absolutePath,
                "-cp", classpath,
            ) + extraOptions.get() + "org.tormap.cli.MainKt"
        )
    }
}
