package com.torusage.database.entities

import javax.persistence.*

@Entity
data class Bridge(
    @Id
    val hashed_fingerprint: String,
    val nickname: String,

    @ElementCollection
    val or_addresses: List<String>,
    val last_seen: String,
    val first_seen: String,
    val running: Boolean,

    @ElementCollection
    val flags: List<String>?,
    val last_restarted: String?,
    val advertised_bandwidth: Int?,
    val platform: String?,
    val version: String?,
    val version_status: String?,
    val recommended_version: Boolean?,

    @ElementCollection
    val transports: List<String>?,
    val bridgedb_distributor: String?,
)
