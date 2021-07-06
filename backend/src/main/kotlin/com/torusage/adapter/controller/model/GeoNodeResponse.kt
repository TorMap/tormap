@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.commaSeperatedToList
import com.torusage.database.entity.GeoNode

/**
 * An API response containing geo nodes to be visualized by frontend world map
 */
class GeoNodeResponse(
    val availableMonths: List<String>,
    val requestedMonth: String,
    rawGeoNodes: Iterable<GeoNode>,
) {
    var geoNodes = rawGeoNodes.map { GeoNodeView(it) }
}

class GeoNodeView(geoNode: GeoNode) {
    val finger = geoNode.id.fingerprint
    val lat = geoNode.latitude
    val lon = geoNode.longitude
    val flags = try {
        geoNode.flags?.commaSeperatedToList()?.map { RelayFlag.valueOf(it).ordinal }
    } catch (exception: Exception) {
        null
    }
}
