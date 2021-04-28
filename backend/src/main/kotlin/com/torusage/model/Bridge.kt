package com.torusage.model

data class Bridge(
    val nickname: String,
    val hashed_fingerprint: String,
    val or_addresses: List<String>,
    val last_seen: String,
    val first_seen: String,
    val running: Boolean,
    val flags: List<String>?,
    val last_restarted: String?,
    val advertised_bandwidth: Int?,
    val platform: String?,
    val version: String?,
    val version_status: String?,
    val recommended_version: Boolean?,
    val transports: List<String>?,
    val bridgedb_distributor: String?,
)
