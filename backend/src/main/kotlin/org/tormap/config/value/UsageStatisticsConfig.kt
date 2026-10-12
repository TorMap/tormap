package org.tormap.config.value

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Where to download Tor Metrics' estimated number of relay users per country from.
 * See [Tor Metrics](https://metrics.torproject.org/stats.html) for the data format.
 */
@ConfigurationProperties("usage-statistics")
data class UsageStatisticsConfig(
    /**
     * The base url of Tor Metrics, only http(s) is accepted
     */
    val baseUrl: String,

    /**
     * The path of the CSV with the estimated relay users per country and day
     */
    val relayUsersPath: String,

    /**
     * The maximum number of bytes that are read from a response. The complete file is about 40 MB.
     */
    val maxDownloadBytes: Long,

    val connectTimeout: Duration,

    /**
     * The time a complete request (including reading the body) may take
     */
    val requestTimeout: Duration,

    /**
     * How many days before the latest stored day are requested again, because Tor Metrics revises recent days
     */
    val refreshOverlapDays: Long,
)
