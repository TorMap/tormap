package com.torusage.adapter.client.model

import com.torusage.database.entity.BridgeSummary
import com.torusage.database.entity.RelaySummary

/**
 * A summary response by the [Onionoo API](https://metrics.torproject.org/onionoo.html#responses) of the Torproject.
 */
data class OnionooSummaryResponse(
    val version: String,
    val build_revision: String?,
    val relays_published: String,
    val relays: List<RelaySummary>,
    val relays_truncated: Int,
    val bridges_published: String,
    val bridges: List<BridgeSummary>,
    val bridges_truncated: Int,
)
