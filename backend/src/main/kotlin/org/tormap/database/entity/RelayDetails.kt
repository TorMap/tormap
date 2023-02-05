package org.tormap.database.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import org.tormap.util.stripLengthForDB
import org.torproject.descriptor.ServerDescriptor
import java.time.LocalDate

/**
 * This entity is used to store details from a relay or bridge [ServerDescriptor].
 * Documentation about attributes can be found at [https://metrics.torproject.org/metrics-lib/org/torproject/descriptor/ServerDescriptor.html].
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@Table("relay_details")
class RelayDetails @PersistenceCreator private constructor(
    @Id private var id: Long? = null,
    var fingerprint: String,
    var month: String,
    var day: LocalDate,
    var autonomousSystemName: String?,
    var autonomousSystemNumber: Int?,
    var address: String,
    var allowSingleHopExits: Boolean,
    var nickname: String,
    var bandwidthRate: Int,
    var bandwidthBurst: Int,
    var bandwidthObserved: Int,
    var platform: String?,
    var protocols: String?,
    var isHibernating: Boolean,
    var uptime: Long?,
    var contact: String?,
    var familyEntries: String?,
    var familyId: Long?,
    var cachesExtraInfo: Boolean,
    var isHiddenServiceDir: Boolean,
    var linkProtocolVersions: String?,
    var circuitProtocolVersions: String?,
    var tunnelledDirServer: Boolean,
) : Persistable<Long> {
    constructor(
        descriptor: ServerDescriptor,
        month: String,
        day: LocalDate,
        autonomousSystemName: String?,
        autonomousSystemNumber: Int?
    ) : this(
        month = month,
        day = day,
        autonomousSystemName = autonomousSystemName,
        autonomousSystemNumber = autonomousSystemNumber,
        address = descriptor.address,
        allowSingleHopExits = descriptor.allowSingleHopExits,
        nickname = descriptor.nickname,
        bandwidthRate = descriptor.bandwidthRate,
        bandwidthBurst = descriptor.bandwidthBurst,
        bandwidthObserved = descriptor.bandwidthObserved,
        platform = descriptor.platform?.stripLengthForDB(),
        protocols = descriptor.protocols
            ?.map { "${it.key} (${it.value.joinToString()})" }
            ?.joinToString()
            ?.stripLengthForDB(),
        fingerprint = descriptor.fingerprint,
        isHibernating = descriptor.isHibernating,
        uptime = descriptor.uptime,
        contact = descriptor.contact?.stripLengthForDB(),
        familyEntries = descriptor.familyEntries?.joinToString(),
        familyId = null,
        cachesExtraInfo = descriptor.cachesExtraInfo,
        isHiddenServiceDir = descriptor.isHiddenServiceDir,
        linkProtocolVersions = descriptor.linkProtocolVersions?.joinToString()?.stripLengthForDB(),
        circuitProtocolVersions = descriptor.circuitProtocolVersions?.joinToString()?.stripLengthForDB(),
        tunnelledDirServer = descriptor.tunnelledDirServer
    )

    override fun getId(): Long? = id

    override fun isNew(): Boolean = id == null
}
