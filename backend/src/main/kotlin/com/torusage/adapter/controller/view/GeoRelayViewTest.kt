@file:Suppress("unused")

package com.torusage.adapter.controller.view

import com.torusage.commaSeparatedToList
import com.torusage.database.entity.GeoRelay
import java.math.BigDecimal

/**
 * A [GeoRelay] with minimal data for fast frontend response times
 */
class GeoRelayViewTest(
    latitude: BigDecimal,
    longitude: BigDecimal,
    countryIsoCode: String?,
    flags: String?,
    id: Long?,
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
    val detailsId = id
}
