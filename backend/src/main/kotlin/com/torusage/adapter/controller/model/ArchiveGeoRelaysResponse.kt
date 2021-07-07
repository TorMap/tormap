@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.commaSeperatedToList
import com.torusage.database.entity.archive.ArchiveGeoRelay

/**
 * An API response containing geo nodes to be visualized by frontend world map
 */
class ArchiveGeoRelaysResponse(
    val availableMonths: List<String>,
    val requestedMonth: String,
    relays: Iterable<ArchiveGeoRelay>,
) {
    var relays = relays.map { ArchiveGeoRelayView(it) }
}

class ArchiveGeoRelayView(relay: ArchiveGeoRelay) {
    val finger = relay.id.fingerprint
    val lat = relay.latitude
    val lon = relay.longitude
    val flags = try {
        relay.flags?.commaSeperatedToList()?.map { RelayFlag.valueOf(it).ordinal }
    } catch (exception: Exception) {
        null
    }
}
