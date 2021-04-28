package com.torusage.adapter

import com.torusage.model.OnionooDetailsResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.*


@Service
class OnionooClient(
    @Value("\${onionoo.baseurl}") private val onionooBaseurl: String,
    webClientBuilder: WebClient.Builder
) {

    private val logger = LoggerFactory.getLogger(OnionooClient::class.java)
    private val webClient: WebClient = webClientBuilder.baseUrl(onionooBaseurl).build()

    /**
     * Get the historic details of Tor relays and/or bridges
     */
    fun getTorNodeDetails(limit: Int? = null, seenSinceUTCDate: Date? = null): OnionooDetailsResponse {
        val resp = webClient.get()
            .uri("/details?limit={limit}", limit)
            .retrieve()
            .bodyToMono(
                OnionooDetailsResponse::class.java
            )

        return resp.block(Duration.ofSeconds(3))
            ?: throw Exception("Could not get tor node details")
    }
}
