@file:Suppress("unused")

package org.tormap.adapter.dto.v1

import io.swagger.v3.oas.annotations.media.Schema
import org.tormap.adapter.dto.RelayDetailsDto
import org.tormap.adapter.dto.RelayIdentifiersDto

/**
 * The details of a relay in a month in the public API v1. Like [RelayDetailsDto], but without internal database IDs:
 * relays are identified by fingerprint. Field names and types are part of the API contract (see api/openapi-v1.json).
 * Boolean fields have no `is` prefix, because Jackson and the OpenAPI generator would name such fields differently.
 *
 * @param familyId the family for `/v1/families/{id}`, null if the relay has no confirmed family
 * @param familyKeys family keys (`ed25519:<family key>`) from the relay's verified family certificates
 *
 * The constructor parameter is hidden: springdoc would document it as a writeOnly `details` field, but this DTO is
 * only ever serialized.
 */
class RelayDetailsV1Dto(@Schema(hidden = true) details: RelayDetailsDto) {
    val fingerprint = details.fingerprint
    val nickname = details.nickname
    val month = details.month
    val day = details.day
    val address = details.address
    val autonomousSystemNumber = details.autonomousSystemNumber
    val autonomousSystemName = details.autonomousSystemName
    val platform = details.platform
    val protocols = details.protocols
    val contact = details.contact
    val uptime = details.uptime
    val bandwidthRate = details.bandwidthRate
    val bandwidthBurst = details.bandwidthBurst
    val bandwidthObserved = details.bandwidthObserved
    val hibernating = details.isHibernating
    val allowSingleHopExits = details.allowSingleHopExits
    val cachesExtraInfo = details.cachesExtraInfo
    val hiddenServiceDir = details.isHiddenServiceDir
    val tunnelledDirServer = details.tunnelledDirServer
    val linkProtocolVersions = details.linkProtocolVersions
    val circuitProtocolVersions = details.circuitProtocolVersions
    val familyEntries = details.familyEntries
    val familyKeys = details.familyKeys
    val familyId = details.familyId
    val confirmedFamilyMembers = details.confirmedFamilyMembers.map(::RelayIdentifiersV1Dto)
    val verifiedHostNames = details.verifiedHostNames
    val unverifiedHostNames = details.unverifiedHostNames
}

/**
 * A relay identified by fingerprint, with its nickname, in the public API v1
 */
class RelayIdentifiersV1Dto(
    val fingerprint: String,
    val nickname: String,
) {
    constructor(identifiers: RelayIdentifiersDto) : this(identifiers.fingerprint, identifiers.nickname)
}
