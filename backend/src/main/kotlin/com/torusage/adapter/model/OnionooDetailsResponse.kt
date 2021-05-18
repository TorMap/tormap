package com.torusage.adapter.model

import com.torusage.database.entities.Bridge
import com.torusage.database.entities.Relay

/**
 * A details response by the [Onionoo API](https://metrics.torproject.org/onionoo.html) of the Torproject.
 */
data class OnionooDetailsResponse(
    val version: String,
    val build_revision: String?,
    val relays_published: String,
    val relays: List<Relay>,
    val bridges_published: String,
    val bridges: List<Bridge>
)
