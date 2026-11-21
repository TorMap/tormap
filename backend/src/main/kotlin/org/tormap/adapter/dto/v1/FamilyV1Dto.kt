@file:Suppress("unused")

package org.tormap.adapter.dto.v1

import org.tormap.adapter.dto.AutonomousSystemDto

/**
 * A family of relays in a month in the public API v1. Field names and types are part of the API contract (see
 * api/openapi-v1.json).
 *
 * @param id the family for `/v1/families/{id}`
 * @param members the relays of the family, ordered by nickname
 * @param autonomousSystems the autonomous systems of the members, ordered by number
 */
class FamilyV1Dto(
    val id: Long,
    val members: List<RelayIdentifiersV1Dto>,
    val autonomousSystems: List<AutonomousSystemDto>,
)

/** One member of a family, as read from the database */
class FamilyMemberRow(
    val familyId: Long,
    val fingerprint: String,
    val nickname: String,
    val autonomousSystemNumber: Int?,
    val autonomousSystemName: String?,
)
