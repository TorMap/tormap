@file:Suppress("unused")

package org.tormap.adapter.dto.v1

import java.time.LocalDate

/**
 * A month in which a relay published a server descriptor, with what it reported in that month, in the public API v1.
 * Field names and types are part of the API contract (see api/openapi-v1.json).
 *
 * @param day the day of the relay's latest server descriptor in the month
 * @param bandwidthObserved bytes per second the relay observed itself
 * @param familyId the family for `/v1/families/{id}`, null if the relay had no confirmed family in the month
 */
class RelayMonthV1Dto(
    val month: String,
    val day: LocalDate,
    val nickname: String,
    val address: String,
    val autonomousSystemNumber: Int?,
    val autonomousSystemName: String?,
    val platform: String?,
    val contact: String?,
    val bandwidthObserved: Int,
    val familyId: Long?,
)
