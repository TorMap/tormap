package org.tormap.database.entity

import org.tormap.util.jointToCommaSeparated
import org.tormap.util.stripLengthForDB
import org.torproject.descriptor.ServerDescriptor
import java.time.LocalDate
import javax.persistence.*

/**
 * This entity is used to store details from a relay or bridge [ServerDescriptor].
 * Documentation about attributes can be found at [https://metrics.torproject.org/metrics-lib/org/torproject/descriptor/ServerDescriptor.html].
 */
@Suppress("unused")
@Entity
@Table(
    indexes = [
        Index(columnList = "month, fingerprint", unique = true),
        Index(columnList = "familyId"),
    ]
)
class RelayDetails(
    @Column(length = 7, columnDefinition = "char(7)")
    var month: String,

    var day: LocalDate,

    @Column(length = 255)
    var autonomousSystemName: String?,

    var autonomousSystemNumber: Int?,

    @Column(length = 15)
    var address: String,

    var allowSingleHopExits: Boolean,

    @Column(length = 19)
    var nickname: String,

    var bandwidthRate: Int,

    var bandwidthBurst: Int,

    var bandwidthObserved: Int,

    var platform: String?,

    var protocols: String?,

    @Column(length = 40, columnDefinition = "char(40)")
    var fingerprint: String,

    var isHibernating: Boolean,

    var uptime: Long?,

    var contact: String?,

    @Lob
    var familyEntries: String?,

    var familyId: Long?,

    var cachesExtraInfo: Boolean,

    var isHiddenServiceDir: Boolean,

    var linkProtocolVersions: String?,

    var circuitProtocolVersions: String?,

    var tunnelledDirServer: Boolean,
) : AbstractBaseEntity<Long>() {

    @Suppress("LeakingThis")
    constructor(
        descriptor: ServerDescriptor,
        month: String,
        day: LocalDate,
        autonomousSystemName: String?,
        autonomousSystemNumber: Int?,
        id: Long?,
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
        platform = descriptor.platform.stripLengthForDB(),
        protocols = descriptor.protocols?.map {
            "${it.key} (${it.value.jointToCommaSeparated()})"
        }?.jointToCommaSeparated().stripLengthForDB(),
        fingerprint = descriptor.fingerprint,
        isHibernating = descriptor.isHibernating,
        uptime = descriptor.uptime,
        contact = descriptor.contact.stripLengthForDB(),
        familyEntries = descriptor.familyEntries?.jointToCommaSeparated(),
        familyId = null,
        cachesExtraInfo = descriptor.cachesExtraInfo,
        isHiddenServiceDir = descriptor.isHiddenServiceDir,
        linkProtocolVersions = descriptor.linkProtocolVersions?.jointToCommaSeparated().stripLengthForDB(),
        circuitProtocolVersions = descriptor.circuitProtocolVersions?.jointToCommaSeparated().stripLengthForDB(),
        tunnelledDirServer = descriptor.tunnelledDirServer
    ) {
        this.id = id
    }
}
