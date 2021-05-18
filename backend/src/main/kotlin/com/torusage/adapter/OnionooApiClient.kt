package com.torusage.adapter

import com.torusage.model.OnionooDetailsResponse
import com.torusage.model.OnionooSummaryResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class OnionooApiClient(
    @Value("\${onionoo.api.baseurl}") private val onionooBaseurl: String,
    webClientBuilder: WebClient.Builder
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(onionooBaseurl).build()

    /**
     * Get the historic details of Tor relays and/or bridges.
     * This uses the function [getOnionnoResponse] which limits the response with parameters [limitCount], [seenSinceUTCDate] and [torNodeType].
     * The response can be quite large due to the full information per node (with no limitation set, currently > 10 MB).
     */
    fun getTorNodeDetails(
        limitCount: Int? = null,
        seenSinceUTCDate: Date? = null,
        torNodeType: TorNodeType? = null,
    ) = getOnionnoResponse("details", OnionooDetailsResponse::class.java, limitCount, seenSinceUTCDate, torNodeType)

    /**
     * Get the historic summary of Tor relays and/or bridges.
     * This uses the function [getOnionnoResponse] which limits the response with parameters [limitCount], [seenSinceUTCDate] and [torNodeType].
     * The response will be smaller due to the decreased information per node.
     */
    fun getTorNodeSummary(
        limitCount: Int? = null,
        seenSinceUTCDate: Date? = null,
        torNodeType: TorNodeType? = null,
    ) = getOnionnoResponse("summary", OnionooSummaryResponse::class.java, limitCount, seenSinceUTCDate, torNodeType)

    /**
     * Get the historic data of Tor relays and/or bridges for an [apiEndpoint] of the [Onionoo API](https://metrics.torproject.org/onionoo.html).
     * The API's JSON response needs to fit the provided [responseClass] to be parsable.
     * The response can be limited by providing a [limitCount] of returned nodes which have been [seenSinceUTCDate] online.
     * The [torNodeType] can be set to only retrieve [TorNodeType.RELAY] or [TorNodeType.BRIDGE].
     * If no [torNodeType] but a [limitCount] is set, relays are returned first.
     * If all limits are set null, all historic nodes provided by the
     * [Onionoo API](https://metrics.torproject.org/onionoo.html) are downloaded.
     *
     */
    private fun <T> getOnionnoResponse(
        apiEndpoint: String,
        responseClass: Class<T>,
        limitCount: Int?,
        seenSinceUTCDate: Date?,
        torNodeType: TorNodeType?,
    ): T {
        val fullUrl = onionooBaseurl + apiEndpoint
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

enum class TorNodeType(val apiReferenceName: String) {
    RELAY("relay"),
    BRIDGE("bridge"),
}
