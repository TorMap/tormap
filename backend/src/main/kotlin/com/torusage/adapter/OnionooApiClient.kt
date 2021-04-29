package com.torusage.adapter

import com.torusage.model.OnionooDetailsResponse
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
     * Get the historic details of Tor relays and/or bridges
     * @param
     */
    fun getTorNodeDetails(
        limit: Int? = null,
        seenSinceUTCDate: Date? = null,
        torNodeType: TorNodeType? = null
    ): OnionooDetailsResponse {
        val uriBuilder: UriBuilder = UriComponentsBuilder.fromUriString("$onionooBaseurl/details")
            .queryParamIfPresent("limit", Optional.ofNullable(limit))
            .queryParamIfPresent("type", Optional.ofNullable(torNodeType?.apiName))

        if(seenSinceUTCDate != null) {
            val millisecondsDifference = Date().time - seenSinceUTCDate.time
            val dayDifference = TimeUnit.DAYS.convert(millisecondsDifference, TimeUnit.MILLISECONDS)
            uriBuilder.queryParam("last_seen_days", "0-$dayDifference")
        }

        val response = webClient.get()
            .uri { uriBuilder.build() }
            .retrieve()
            .bodyToMono(
                OnionooDetailsResponse::class.java
            )

        return response.block(Duration.ofSeconds(60))
            ?: throw Exception("Could not get tor node details!")
    }
}

enum class TorNodeType(val apiName: String) {
    RELAY("relay"),
    BRIDGE("bridge"),
}
