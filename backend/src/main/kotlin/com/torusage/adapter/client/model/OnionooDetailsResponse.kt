package com.torusage.adapter.client.model

/**
 * A details response by the [Onionoo API](https://metrics.torproject.org/onionoo.html#responses) of the Torproject.
 */
data class OnionooDetailsResponse(
    val version: String,
    val build_revision: String?,
    val relays_published: String,
    val relays: List<Any>,
    val bridges_published: String,
    val bridges: List<Any>,
)
