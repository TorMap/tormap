@file:Suppress("unused")

package org.tormap.adapter.dto.v1

import org.tormap.database.entity.TorRelayFlag
import org.tormap.util.commaSeparatedToList
import java.math.BigDecimal

/**
 * A relay of a day in the public API v1. Field names and types are part of the API contract (see api/openapi-v1.json):
 * only add fields, never rename or remove them. Relays are identified by fingerprint, not by internal database IDs.
 *
 * @param familyId the family for `/v1/families/{id}`, null if the relay has no confirmed family
 * @param flags the names of the relay's flags, e.g. "Exit" or "Guard"
 */
class RelayV1Dto(
    val fingerprint: String,
    val nickname: String?,
    val country: String,
    latitude: BigDecimal,
    longitude: BigDecimal,
    flags: String?,
    val consensusWeight: Long?,
    val autonomousSystemNumber: Int?,
    val familyId: Long?,
) {
    val lat = latitude
    val long = longitude
    val flags: List<String> = flags
        ?.commaSeparatedToList()
        ?.mapNotNull { ordinal -> ordinal.toIntOrNull()?.let { TorRelayFlag.entries.getOrNull(it)?.name } }
        .orEmpty()
}
