package com.torusage.adapter.model

import com.torusage.database.entities.Bridge
import com.torusage.database.entities.RelaySummary

/**
 * A summary response by the [Onionoo API](https://metrics.torproject.org/onionoo.html) of the Torproject.
 */
data class OnionooSummaryResponse(
    val version: String,
    val build_revision: String?,
    val relays_published: String,
    val relays: List<RelaySummary>,
    val relays_truncated: Int,
    val bridges_published: String,
    val bridges: List<Bridge>,
    val bridges_truncated: Int,
)
