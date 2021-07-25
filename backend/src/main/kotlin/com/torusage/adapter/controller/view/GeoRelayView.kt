@file:Suppress("unused")

package com.torusage.adapter.controller.view

import com.torusage.commaSeparatedToList
import com.torusage.database.entity.GeoRelay

/**
 * A [GeoRelay] with minimal data for fast frontend response times
 */
class GeoRelayView(relay: GeoRelay) {
    val lat = relay.latitude
    val long = relay.longitude
    val country = relay.countryIsoCode
    val flags = try {
        relay.flags?.commaSeparatedToList()?.map { it.toInt() }
    } catch (exception: Exception) {
        null
    }
    val detailsId = relay.nodeDetailsId
    val familyId = relay.nodeFamilyId
}
