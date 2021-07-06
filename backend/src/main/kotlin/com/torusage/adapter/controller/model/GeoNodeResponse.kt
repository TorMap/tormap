@file:Suppress("unused")

package com.torusage.adapter.controller.model

import com.torusage.database.entity.GeoNode

/**
 * An API response containing geo nodes to be visualized by frontend world map
 */
class GeoNodeResponse(
    val availableMonths: List<String>,
    val requestedMonth: String,
    val geoNodes: Iterable<GeoNode>,
)
