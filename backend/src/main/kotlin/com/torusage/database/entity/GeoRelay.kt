package com.torusage.database.entity

import org.torproject.descriptor.NetworkStatusEntry
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.*

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Suppress("unused")
@Entity
@Table(
    indexes = [
        Index(columnList = "fingerprint, day", unique = true, name = "fingerprint_day_index"),
        Index(columnList = "day", name = "day_index"),
    ]
)
class GeoRelay(
    networkStatusEntry: NetworkStatusEntry,
    var day: LocalDate,
    var latitude: BigDecimal,
    var longitude: BigDecimal,
    var countryIsoCode: String?,

    @Id
    @GeneratedValue
    val id: Long? = null,
) {
    @Column(length = 40)
    var fingerprint: String = networkStatusEntry.fingerprint

    var flags: String? = try {
        networkStatusEntry.flags.map { TorRelayFlag.valueOf(it.toString()).ordinal }.joinToString(", ")
    } catch (exception: Exception) {
        null
    }

    val nodeDetailsId: Long? = null

    val nodeFamilyId: Long? = null
}

/**
 * The possible flags a relay can have assigned to it
 * [Further documentation](https://github.com/torproject/torspec/blob/main/dir-spec.txt)
 * Please keep the order of attributes, since the frontend and DB rely on the exact order.
 */
enum class TorRelayFlag {
    Valid, // if the router has been 'validated'
    Named,
    Unnamed,
    Running, // if the router is currently usable over all its published ORPorts
    Stable, // if the router is suitable for long-lived circuits
    Exit, // if the router is more useful for building general-purpose exit circuits than for relay circuits
    Fast, // if the router is suitable for high-bandwidth circuits
    Guard, // if the router is suitable for use as an entry guard
    Authority, // if the router is a directory authority
    V2Dir, // if the router implements the v2 directory protocol or higher
    HSDir, // if the router is considered a v2 hidden service directory
    NoEdConsensus, // if any Ed25519 key in the router's descriptor or microdesriptor does not reflect authority consensus
    StaleDesc, // if the router should upload a new descriptor because the old one is too old
    Sybil,
    BadExit, // if the router is believed to be useless as an exit node
}