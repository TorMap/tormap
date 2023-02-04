package org.tormap.adapter.dto

import org.tormap.database.entity.RelayLocation
import java.math.BigDecimal

/**
 * A [RelayLocation] with minimal data for fast frontend response times
 */
data class RelayLocationDto(
    val lat: BigDecimal,
    val long: BigDecimal,
    val country: String,
    val flags: List<Int>?,
    val detailsId: Long?,
    val familyId: Long?,
)
