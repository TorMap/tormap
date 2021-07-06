@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.database.entity.Relay

/**
 * A relay with minimal data to be displayed in the frontend world map
 */
class RelayResponse(relay: Relay) {
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
 */
enum class RelayFlag {
    Valid,
    Named,
    Unamed,
    Running,
    Stable,
    Exit,
    Fast,
    Guard,
    Authority,
    V2Dir,
    HSDir,
    NoEdConsensus,
    StaleDesc,
    Sybil,
    BadExit,
}
