package com.torusage.model


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