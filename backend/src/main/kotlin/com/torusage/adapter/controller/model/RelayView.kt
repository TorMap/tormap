@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.database.entity.recent.Relay

/**
 * A [Relay] with minimal data for fast frontend response times
 */
class RelayView(relay: Relay) {
    val id = relay.id
    val firstSeen = stripTimeOfDate(relay.first_seen)
    val lastSeen = stripTimeOfDate(relay.last_seen)
    val lat = relay.latitude
    val long = relay.longitude
    val flags = relay.flags?.map { RelayFlag.valueOf(it).ordinal }

    private fun stripTimeOfDate(date: String) = date.replace("\\s.*".toRegex(), "")
}

/**
 * The possible flags a relay can have assigned to it
 * [Further documentation](https://github.com/torproject/torspec/blob/main/dir-spec.txt)
 * Please keep the order of attributes, since the frontend and DB rely on the exact order.
 */
enum class RelayFlag {
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
