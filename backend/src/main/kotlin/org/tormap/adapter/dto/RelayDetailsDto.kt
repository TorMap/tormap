@file:Suppress("unused")

package org.tormap.adapter.dto

import org.tormap.database.entity.RelayDetails
import org.tormap.service.ReverseDnsLookupResult

class RelayDetailsDto(
    details: RelayDetails,
    confirmedFamilyMembers: List<RelayIdentifiersDto>,
    reverseDnsLookupResult: ReverseDnsLookupResult,
) {
    val id = details.id!!
    val month = details.month
    val day = details.day
    val address = details.address
    val autonomousSystemName = details.autonomousSystemName
    val autonomousSystemNumber = details.autonomousSystemNumber
    val allowSingleHopExits = details.allowSingleHopExits
    val nickname = details.nickname
    val bandwidthRate = details.bandwidthRate
    val bandwidthBurst = details.bandwidthBurst
    val bandwidthObserved = details.bandwidthObserved
    val platform = details.platform
    val protocols = details.protocols
    val fingerprint = details.fingerprint
    val isHibernating = details.isHibernating
    val uptime = details.uptime
    val contact = details.contact
    val familyEntries = details.familyEntries
    val familyId = details.familyId
    val cachesExtraInfo = details.cachesExtraInfo
    val isHiddenServiceDir = details.isHiddenServiceDir
    val linkProtocolVersions = details.linkProtocolVersions
    val circuitProtocolVersions = details.circuitProtocolVersions
    val tunnelledDirServer = details.tunnelledDirServer
    val confirmedFamilyMembers = confirmedFamilyMembers
    val verifiedHostNames = reverseDnsLookupResult.verifiedHostNames
    val unverifiedHostNames = reverseDnsLookupResult.unverifiedHostNames
}
