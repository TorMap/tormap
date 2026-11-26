// Google's mirror of Maven Central comes first: Maven Central rate-limits shared IP addresses (CI runners, Renovate),
// which fails builds with HTTP 429. Same artifacts; Maven Central and the plugin portal stay as fallbacks.
pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "tormap-cli"
