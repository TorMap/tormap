package com.torusage.database.entity.recent

import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id

/**
 * This entity matches the [Bridge details object](https://metrics.torproject.org/onionoo.html#details_bridge)
 * of the Onionoo API and is also used to generate the DB structure.
 */
@Suppress("unused")
@Entity
data class Bridge(
    /** Identification */
    @Id
    val hashed_fingerprint: String,

    val nickname: String,

    /** Networking */
    @ElementCollection
    val or_addresses: List<String>,

    val last_seen: String,

    val first_seen: String,

    val running: Boolean,

    /** Information */
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
