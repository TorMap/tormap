package org.tormap.database.entity

import org.tormap.jointToCommaSeparated
import org.tormap.stripLengthForDB
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
    descriptor: ServerDescriptor,

    @Column(length = 7, columnDefinition = "char(7)")
    var month: String,

    var day: LocalDate,

    @Column(length = 255)
    var autonomousSystemName: String? = null,

    var autonomousSystemNumber: Int? = null,

    @Id
    @GeneratedValue
    val id: Long? = null,
) {
    @Column(length = 15)
    var address: String? = descriptor.address

    var allowSingleHopExits: Boolean = descriptor.allowSingleHopExits

    @Column(length = 19)
    var nickname: String? = descriptor.nickname

    var bandwidthRate: Int = descriptor.bandwidthRate

    var bandwidthBurst: Int = descriptor.bandwidthBurst

    var bandwidthObserved: Int = descriptor.bandwidthObserved

    var platform: String? = descriptor.platform.stripLengthForDB()

    var protocols: String? = descriptor.protocols?.map {
        "${it.key} (${it.value.jointToCommaSeparated()})"
    }?.jointToCommaSeparated().stripLengthForDB()

    @Column(length = 40, columnDefinition = "char(40)")
    var fingerprint: String = descriptor.fingerprint

    var isHibernating: Boolean = descriptor.isHibernating

    var uptime: Long? = descriptor.uptime

    var contact: String? = descriptor.contact.stripLengthForDB()

    @Lob
    var familyEntries: String? = descriptor.familyEntries?.jointToCommaSeparated()

    var familyId: Long? = null

    var cachesExtraInfo: Boolean = descriptor.cachesExtraInfo

    var isHiddenServiceDir: Boolean = descriptor.isHiddenServiceDir

    var linkProtocolVersions: String? = descriptor.linkProtocolVersions?.jointToCommaSeparated().stripLengthForDB()

    var circuitProtocolVersions: String? =
        descriptor.circuitProtocolVersions?.jointToCommaSeparated().stripLengthForDB()

    var tunnelledDirServer: Boolean = descriptor.tunnelledDirServer

}