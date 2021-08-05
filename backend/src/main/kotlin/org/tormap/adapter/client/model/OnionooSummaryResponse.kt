package org.tormap.adapter.client.model

/**
 * A summary response by the [Onionoo API](https://metrics.torproject.org/onionoo.html#responses) of the Torproject.
 */
data class OnionooSummaryResponse(
    val version: String,
    val build_revision: String?,
    val relays_published: String,
    val relays: List<Any>,
    val relays_truncated: Int,
    val bridges_published: String,
    val bridges: List<Any>,
    val bridges_truncated: Int,
)
