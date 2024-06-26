@file:Suppress("unused")

package org.tormap.adapter.dto

import org.tormap.database.entity.RelayLocation
import org.tormap.util.commaSeparatedToList
import java.math.BigDecimal

/**
 * A [RelayLocation] with minimal data for fast frontend response times
 */
class RelayLocationDto(
    latitude: BigDecimal,
    longitude: BigDecimal,
    countryCode: String,
    flags: String?,
    val detailsId: Long?,
    val familyId: Long?,
    val nickname: String?,
) {
    val lat = latitude
    val long = longitude
    val country = countryCode
    val flags = try {
        flags?.commaSeparatedToList()?.map { it.toInt() }
    } catch (exception: Exception) {
        null
    }
}
