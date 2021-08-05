package org.tormap.adapter.client

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriComponentsBuilder
import org.tormap.adapter.client.model.OnionooDetailsResponse
import org.tormap.adapter.client.model.OnionooSummaryResponse
import org.tormap.config.ApiConfig
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This is an client for downloading data regarding the Tor network from the
 * [Onionoo API](https://metrics.torproject.org/onionoo.html).
 * Currently the client is not used, but it is ready if needed in the future.
 */
@Suppress("unused")
@Service
class OnionooApiClient(
    webClientBuilder: WebClient.Builder,
    private val apiConfig: ApiConfig,
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(apiConfig.onionooBaseURL).build()

    /**
     * Get the details of Tor relays and/or bridges.
     * This is a wrapper function for [getOnionnoResponse].
     * The response will be larger due to the full information per node (in 2021 roughly 12 MB in total size).
     */
    fun getTorNodeDetails(
        limitCount: Int? = null,
        seenSinceUTCDate: Date? = null,
        torNodeType: TorNodeType? = null,
    ) = getOnionnoResponse("details", OnionooDetailsResponse::class.java, limitCount, seenSinceUTCDate, torNodeType)

    /**
     * Get the summary of Tor relays and/or bridges.
     * This is a wrapper function for [getOnionnoResponse].
     * The response will be smaller due to the decreased information per node.
     */
    fun getTorNodeSummary(
        limitCount: Int? = null,
        seenSinceUTCDate: Date? = null,
        torNodeType: TorNodeType? = null,
    ) = getOnionnoResponse("summary", OnionooSummaryResponse::class.java, limitCount, seenSinceUTCDate, torNodeType)

    /**
     * Get data of Tor relays and/or bridges for an [apiEndpoint] of the [Onionoo API](https://metrics.torproject.org/onionoo.html).
     * The API's JSON response needs to fit the provided [responseClass] to be parsable.
     * The response can be limited by providing a [limitCount] of returned nodes
     * which have been [seenSinceUTCDate] online (only nodes seen in the last week can be returned).
     * The [torNodeType] can be set to only retrieve [TorNodeType.RELAY] or [TorNodeType.BRIDGE].
     * If no [torNodeType] but a [limitCount] is set, relays are returned first.
     * If no limits are provided, all nodes seen in the last week will be downloaded.
     */
    fun <T> getOnionnoResponse(
        apiEndpoint: String,
        responseClass: Class<T>,
        limitCount: Int?,
        seenSinceUTCDate: Date?,
        torNodeType: TorNodeType?,
    ): T {
        val fullUrl = apiConfig.onionooBaseURL + apiEndpoint
        val uriBuilder: UriBuilder = UriComponentsBuilder.fromUriString(fullUrl)
            .queryParamIfPresent("limit", Optional.ofNullable(limitCount))
            .queryParamIfPresent("type", Optional.ofNullable(torNodeType?.apiReferenceName))

        if (seenSinceUTCDate != null) {
            val millisecondsDifference = Date().time - seenSinceUTCDate.time
            val dayDifference = TimeUnit.DAYS.convert(millisecondsDifference, TimeUnit.MILLISECONDS)
            uriBuilder.queryParam("last_seen_days", "0-$dayDifference")
        }

        val response = webClient.get()
            .uri { uriBuilder.build() }
            .retrieve()
            .bodyToMono(
                responseClass
            )

        return response.block(Duration.ofSeconds(60))
            ?: throw Exception("Could not get Onionoo response from endpoint '$apiEndpoint'!")
    }
}

/**
 * Tor nodes fetched from the Onionoo API belong in one of the categories "relay" or "bridge".
 * [See differences](https://community.torproject.org/relay/types-of-relays/)
 */
enum class TorNodeType(val apiReferenceName: String) {
    RELAY("relay"),
    BRIDGE("bridge"),
}
