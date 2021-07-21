package com.torusage.database.entity.archive

import com.torusage.jointToCommaSeparated
import org.torproject.descriptor.ServerDescriptor
import java.time.LocalDate
import javax.persistence.*

/**
 * This entity is used to store relevant information about a [ServerDescriptor]
 */
@Suppress("unused")
@Entity
@Table(
    indexes = [
        Index(columnList = "fingerprint, day", unique = true),
        Index(columnList = "day"),
    ]
)
class ArchiveNodeDetails(
    descriptor: ServerDescriptor,
    var day: LocalDate,
) {
    @Id
    @GeneratedValue
    val id: Long? = null

    var digestSha1Hex: String? = descriptor.digestSha1Hex

    var digestSha256Base64: String? = descriptor.digestSha256Base64

    @Column(length = 19)
    var nickname: String? = descriptor.nickname

    var address: String? = descriptor.address

    var orPort: Int = descriptor.orPort

    var socksPort: Int = descriptor.socksPort

    var dirPort: Int = descriptor.dirPort

    var orAddresses: String? = descriptor.orAddresses.jointToCommaSeparated()

    var bandwidthRate: Int = descriptor.bandwidthRate

    var bandwidthBurst: Int = descriptor.bandwidthBurst

    var bandwidthObserved: Int = descriptor.bandwidthObserved

    var platform: String? = descriptor.platform

    var protocols: String? = descriptor.protocols?.map {
        it.key + it.value.jointToCommaSeparated()
    }.jointToCommaSeparated()

    @Column(length = 40)
    var fingerprint: String? = descriptor.fingerprint

    var isHibernating: Boolean = descriptor.isHibernating

    var uptime: Long = descriptor.uptime

    var onionKey: String? = descriptor.onionKey

    var signingKey: String? = descriptor.signingKey

    @Lob
    var exitPolicyLines: String? = descriptor.exitPolicyLines.jointToCommaSeparated()

    @Lob
    var routerSignature: String? = descriptor.routerSignature

    @Lob
    var contact: String? = descriptor.contact

    var bridgeDistributionRequest: String? = descriptor.bridgeDistributionRequest

    @Lob
    var familyEntries: String? = descriptor.familyEntries.jointToCommaSeparated()

    var usesEnhancedDnsLogic: Boolean = descriptor.usesEnhancedDnsLogic

    var cachesExtraInfo: Boolean = descriptor.cachesExtraInfo

    var extraInfoDigestSha1Hex: String? = descriptor.extraInfoDigestSha1Hex

    var extraInfoDigestSha256Base64: String? = descriptor.extraInfoDigestSha256Base64

    var isHiddenServiceDir: Boolean = descriptor.isHiddenServiceDir

    var linkProtocolVersions: String? = descriptor.linkProtocolVersions.jointToCommaSeparated()

    var circuitProtocolVersions: String? = descriptor.circuitProtocolVersions.jointToCommaSeparated()

    var allowSingleHopExits: Boolean = descriptor.allowSingleHopExits

    var ipv6DefaultPolicy: String? = descriptor.ipv6DefaultPolicy

    @Lob
    var ipv6PortList: String? = descriptor.ipv6PortList

    var ntorOnionKey: String? = descriptor.ntorOnionKey

    var identityEd25519: String? = descriptor.identityEd25519

    var masterKeyEd25519: String? = descriptor.masterKeyEd25519

    @Lob
    var routerSignatureEd25519: String? = descriptor.routerSignatureEd25519

    @Lob
    var onionKeyCrossCert: String? = descriptor.onionKeyCrosscert

    @Lob
    var ntorOnionKeyCrossCert: String? = descriptor.ntorOnionKeyCrosscert

    var ntorOnionKeyCrossCertSign: Int = descriptor.ntorOnionKeyCrosscertSign

    var tunnelledDirServer: Boolean = descriptor.tunnelledDirServer
}
