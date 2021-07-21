@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.database.entity.archive.TorRelayFlag
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
    val flags = relay.flags?.map { TorRelayFlag.valueOf(it).ordinal }

    private fun stripTimeOfDate(date: String) = date.replace("\\s.*".toRegex(), "")
}
