package org.tormap.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Caches large API responses (the list of days and the relays of a day) as gzip files, so repeated commands and
 * filters work without downloading a day again. Recent days are refreshed after an hour, because the server still
 * adds descriptors to them; older days are kept for 30 days.
 */
class ResponseCache(private val directory: Path, private val clock: Clock = Clock.systemUTC()) {
    companion object {
        val SHORT: Duration = Duration.ofHours(1)
        private val LONG: Duration = Duration.ofDays(30)

        fun ttlForDay(day: String, today: LocalDate = LocalDate.now(ZoneOffset.UTC)): Duration =
            if (LocalDate.parse(day) < today.minusDays(3)) LONG else SHORT

        /** `$XDG_CACHE_HOME/tormap`, `%LOCALAPPDATA%\tormap\cache` on Windows, or `~/.cache/tormap`. */
        fun defaultDirectory(env: (String) -> String?): Path {
            env("XDG_CACHE_HOME")?.takeIf { it.isNotBlank() }?.let { return Path.of(it, "tormap") }
            env("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { return Path.of(it, "tormap", "cache") }
            return Path.of(System.getProperty("user.home"), ".cache", "tormap")
        }
    }

    private fun fileOf(key: String): Path {
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return directory.resolve("$hash.json.gz")
    }

    fun read(key: String, ttl: Duration): String? = try {
        val file = fileOf(key)
        val age = Duration.between(Files.getLastModifiedTime(file).toInstant(), clock.instant())
        if (age > ttl || age.isNegative) null else GZIPInputStream(Files.newInputStream(file)).use { String(it.readAllBytes()) }
    } catch (_: Exception) {
        // A missing or damaged entry is downloaded again
        null
    }

    fun write(key: String, body: String) {
        try {
            Files.createDirectories(directory)
            val temporary = Files.createTempFile(directory, "download", ".tmp")
            GZIPOutputStream(Files.newOutputStream(temporary)).use { it.write(body.toByteArray()) }
            Files.move(temporary, fileOf(key), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            // Caching is an optimization: a read-only or full disk must not break the command
        }
    }
}
