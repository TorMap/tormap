@file:Suppress("unused")

package org.tormap.adapter.controller.view

import org.tormap.commaSeparatedToList
import org.tormap.database.entity.GeoRelay
import java.math.BigDecimal

/**
 * A [GeoRelay] with minimal data for fast frontend response times
 */
class GeoRelayView(
    latitude: BigDecimal,
    longitude: BigDecimal,
    countryIsoCode: String?,
    flags: String?,
    val detailsId: Long?,
    val familyId: Long?,
) {
    val lat = latitude
    val long = longitude
    val country = countryIsoCode
    val flags = try {
        flags?.commaSeparatedToList()?.map { it.toInt() }
    } catch (exception: Exception) {
        null
    }
}
