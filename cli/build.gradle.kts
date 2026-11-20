import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "org.tormap"
// Shared with backend/build.gradle.kts and frontend/package.json: bump all three before tagging a release
version = "4.2.2"
java.sourceCompatibility = JavaVersion.VERSION_25

plugins {
    kotlin("jvm") version "2.4.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Command line parsing without the Mordant terminal library https://ajalt.github.io/clikt/
    implementation("com.github.ajalt.clikt:clikt-core:5.2.0")

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
