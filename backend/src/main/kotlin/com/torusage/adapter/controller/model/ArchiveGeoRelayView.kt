@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.commaSeparatedToList
import com.torusage.database.entity.archive.ArchiveGeoRelay

/**
 * A [ArchiveGeoRelay] with minimal data for fast frontend response times
 */
class ArchiveGeoRelayView(relay: ArchiveGeoRelay) {
    val id = relay.id
    val lat = relay.latitude
    val long = relay.longitude
    val flags = try {
        relay.flags?.commaSeparatedToList()
    } catch (exception: Exception) {
        null
    }
}
