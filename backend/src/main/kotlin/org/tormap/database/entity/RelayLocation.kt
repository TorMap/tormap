package org.tormap.database.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import org.torproject.descriptor.NetworkStatusEntry
import java.math.BigDecimal
import java.time.LocalDate

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Table("relay_location")
@Suppress("unused", "MemberVisibilityCanBePrivate")
class RelayLocation @PersistenceCreator private constructor(
    @Id private var id: Long? = null,
    var fingerprint: String,
    var day: LocalDate,
    var flags: Set<TorRelayFlag>?,
    var latitude: BigDecimal,
    var longitude: BigDecimal,
    var countryCode: String,
) : Persistable<Long?> {
    constructor(
        networkStatusEntry: NetworkStatusEntry,
        day: LocalDate,
        latitude: BigDecimal,
        longitude: BigDecimal,
        countryCode: String
    ) : this(
        fingerprint = networkStatusEntry.fingerprint,
        day = day,
        flags = kotlin.runCatching { networkStatusEntry.flags.map(TorRelayFlag::valueOf).toSet() }.getOrNull(),
        latitude = latitude,
        longitude = longitude,
        countryCode = countryCode
    )

    override fun getId(): Long? = id

    override fun isNew(): Boolean = id == null
}

/**
 * The possible flags a relay can have assigned to it
 * [Further documentation](https://github.com/torproject/torspec/blob/main/dir-spec.txt)
 * Please keep the order of attributes, since the frontend and DB rely on the exact order.
 */
enum class TorRelayFlag {
    Valid, // if the router has been 'validated'
    Named, // If the router has a nickname
    Unnamed, // If the router has no nickname
    Running, // if the router is currently usable over all its published ORPorts
    Stable, // if the router is suitable for long-lived circuits
    Exit, // if the router is more useful for building general-purpose exit circuits than for relay circuits
    Fast, // if the router is suitable for high-bandwidth circuits
    Guard, // if the router is suitable for use as an entry guard
    Authority, // if the router is a directory authority
    V2Dir, // if the router implements the v2 directory protocol or higher
    HSDir, // if the router is considered a v2 hidden service directory
    NoEdConsensus, // if any Ed25519 key in the router's descriptor or micro descriptor does not reflect authority consensus
    StaleDesc, // if the router should upload a new descriptor because the old one is too old
    Sybil, // If more than 2 relays run on the same IP
    BadExit, // if the router is believed to be useless as an exit node
}
