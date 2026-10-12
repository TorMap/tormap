package org.tormap.service

import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.tormap.adapter.dto.CountryUsageRow
import org.tormap.config.CacheConfig
import org.tormap.config.value.UsageStatisticsConfig
import org.tormap.database.repository.CountryUsageRepository
import org.tormap.util.UserStatsCsvParser
import org.tormap.util.logger
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Imports the estimated number of relay users per country from Tor Metrics.
 *
 * The first import downloads the complete history (~40 MB), later imports only request the days since the latest
 * stored day, with some overlap because Tor Metrics revises recent estimates.
 */
@Service
class CountryUsageService(
    private val config: UsageStatisticsConfig,
    private val countryUsageRepository: CountryUsageRepository,
    private val cacheManager: CacheManager,
) {
    private val logger = logger()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(config.connectTimeout)
        // The host is configured and trusted, a redirect to another host is not
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    /**
     * Downloads and stores the estimates that are not stored yet. Failures are logged and retried by the next run.
     * @return the number of stored rows
     */
    fun refresh(): Int {
        return try {
            val latestDay = countryUsageRepository.findLatestDay()
            val storedRows = download(buildUri(latestDay?.minusDays(config.refreshOverlapDays)))
            if (storedRows > 0) {
                cacheManager.getCache(CacheConfig.COUNTRY_USAGE_PER_DAY)?.clear()
            }
            logger.info("Imported {} country usage rows", storedRows)
            storedRows
        } catch (exception: Exception) {
            logger.error("Could not import country usage from Tor Metrics! {}", exception.message)
            0
        }
    }

    private fun buildUri(start: LocalDate?): URI {
        val uri = URI.create(config.baseUrl.trimEnd('/') + config.relayUsersPath + (start?.let { "?start=$it" } ?: ""))
        require(uri.scheme == "http" || uri.scheme == "https") { "Only http(s) is supported" }
        require(!uri.host.isNullOrBlank()) { "The url has no host" }
        return uri
    }

    private fun download(uri: URI): Int {
        val request = HttpRequest.newBuilder(uri)
            .timeout(config.requestTimeout)
            .header("Accept", "text/csv")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { body ->
            check(response.statusCode() == 200) { "Unexpected status ${response.statusCode()}" }
            var malformedLines = 0
            val latestAcceptableDay = LocalDate.now(ZoneOffset.UTC).plusDays(1)
            var storedRows = 0
            LimitedInputStream(body, config.maxDownloadBytes).bufferedReader().useLines { lines ->
                UserStatsCsvParser.parse(lines) { malformedLines++ }
                    // An estimate can not be from the future
                    .filter { !it.day.isAfter(latestAcceptableDay) }
                    .chunked(UPSERT_CHUNK_SIZE)
                    .forEach { rows: List<CountryUsageRow> ->
                        countryUsageRepository.upsertAll(rows)
                        storedRows += rows.size
                    }
            }
            if (malformedLines > 0) {
                logger.warn("Skipped {} malformed lines of the Tor Metrics country usage", malformedLines)
            }
            return storedRows
        }
    }

    /**
     * Fails once more than [limit] bytes were read, so a wrong or manipulated response can not fill the memory or disk.
     */
    private class LimitedInputStream(delegate: InputStream, private val limit: Long) : FilterInputStream(delegate) {
        private var bytesRead = 0L

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) count(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val read = super.read(buffer, offset, length)
            if (read > 0) count(read.toLong())
            return read
        }

        private fun count(bytes: Long) {
            bytesRead += bytes
            if (bytesRead > limit) throw IOException("The response is larger than $limit bytes")
        }
    }

    private companion object {
        const val UPSERT_CHUNK_SIZE = 2000
    }
}
