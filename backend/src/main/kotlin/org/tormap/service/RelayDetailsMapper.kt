package org.tormap.service

import org.tormap.database.entity.RelayDetails

fun RelayDetails?.map(details: RelayDetails): RelayDetails = this?.apply {
    month = details.month
    day = details.day
    autonomousSystemName = details.autonomousSystemName
    autonomousSystemNumber = details.autonomousSystemNumber
    address = details.address
    allowSingleHopExits = details.allowSingleHopExits
    nickname = details.nickname
    bandwidthRate = details.bandwidthRate
    bandwidthBurst = details.bandwidthBurst
    bandwidthObserved = details.bandwidthObserved
    platform = details.platform
    protocols = details.protocols
    fingerprint = details.fingerprint
    isHibernating = details.isHibernating
    uptime = details.uptime
    contact = details.contact
    familyEntries = details.familyEntries
    familyId = details.familyId
    cachesExtraInfo = details.cachesExtraInfo
    isHiddenServiceDir = details.isHiddenServiceDir
    linkProtocolVersions = details.linkProtocolVersions
    circuitProtocolVersions = details.circuitProtocolVersions
    tunnelledDirServer = details.tunnelledDirServer
} ?: details
