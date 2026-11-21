package org.tormap.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
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
 * adds descriptors to them; older days are kept for 30 days. An expired entry is revalidated with its ETag, so an
 * unchanged day is not downloaded again.
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

    /** A cached response. A stale one can still be used after the server answered 304 Not Modified to its ETag. */
    class Entry(val body: String, val eTag: String?, val fresh: Boolean)

    private fun fileOf(key: String, extension: String = "json.gz"): Path {
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return directory.resolve("$hash.$extension")
    }

    fun read(key: String, ttl: Duration): Entry? = try {
        val file = fileOf(key)
        val age = Duration.between(Files.getLastModifiedTime(file).toInstant(), clock.instant())
        val body = GZIPInputStream(Files.newInputStream(file)).use { String(it.readAllBytes()) }
        val eTag = try {
            Files.readString(fileOf(key, "etag")).takeIf(::isValidETag)
        } catch (_: Exception) {
            null
        }
        Entry(body, eTag, fresh = age <= ttl && !age.isNegative)
    } catch (_: Exception) {
        // A missing or damaged entry is downloaded again
        null
    }

    fun write(key: String, body: String, eTag: String? = null) {
        try {
            Files.createDirectories(directory)
            val eTagFile = fileOf(key, "etag")
            // The old ETag must not outlive its body, so it goes first and the new one is written last
            Files.deleteIfExists(eTagFile)
            val temporary = Files.createTempFile(directory, "download", ".tmp")
            GZIPOutputStream(Files.newOutputStream(temporary)).use { it.write(body.toByteArray()) }
            Files.move(temporary, fileOf(key), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            if (eTag != null && isValidETag(eTag)) Files.writeString(eTagFile, eTag)
        } catch (_: Exception) {
            // Caching is an optimization: a read-only or full disk must not break the command
        }
    }

    /** Marks an entry as fresh again, after the server confirmed that it did not change. */
    fun refresh(key: String) {
        try {
            Files.setLastModifiedTime(fileOf(key), FileTime.from(clock.instant()))
        } catch (_: Exception) {
            // The entry is then revalidated again next time
        }
    }
}

/** An ETag is sent back to the server as a header, so only short, printable values are kept. */
internal fun isValidETag(value: String) = value.length in 1..256 && value.all { it in '\u0021'..'\u007e' }
