@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.commaSeparatedToList
import com.torusage.database.entity.archive.ArchiveGeoRelay

/**
 * A [ArchiveGeoRelay] with minimal data for fast frontend response times
 */
class ArchiveGeoRelayView(relay: ArchiveGeoRelay) {
    val finger = relay.id.fingerprint
    val lat = relay.latitude
    val long = relay.longitude
    val flags = try {
        relay.flags?.commaSeparatedToList()?.map { RelayFlag.valueOf(it).ordinal }
    } catch (exception: Exception) {
        null
    }
}