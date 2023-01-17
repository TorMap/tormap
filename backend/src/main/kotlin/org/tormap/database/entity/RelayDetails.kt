package org.tormap.database.entity

import org.tormap.adapter.dto.RelayFamilyIdentifiersDto
import org.tormap.util.jointToCommaSeparated
import org.tormap.util.stripLengthForDB
import org.torproject.descriptor.ServerDescriptor
import java.time.LocalDate
import javax.persistence.*

const val FIND_FAMILY_IDENTIFIERS_QUERY = "findFamilyIdentifiers"

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
@SqlResultSetMapping(
    name = FIND_FAMILY_IDENTIFIERS_QUERY,
    classes = [
        ConstructorResult(
            targetClass = RelayFamilyIdentifiersDto::class,
            columns = [
                ColumnResult(name = "id", type = Long::class),
                ColumnResult(name = "memberCount", type = Long::class),
                ColumnResult(name = "nicknames", type = String::class),
                ColumnResult(name = "autonomousSystems", type = String::class),
            ]
        )
    ]
)
@NamedNativeQuery(
    name = FIND_FAMILY_IDENTIFIERS_QUERY,
    query = "SELECT " +
        "family_id as id, " +
        "count(id) as memberCount, " +
        "string_agg(nickname, ', ') as nicknames, " +
        "string_agg(DISTINCT autonomous_system_name, ', ') as autonomousSystems " +
        "FROM relay_details " +
        "WHERE family_id in :familyIds " +
        "GROUP BY family_id",
    resultClass = RelayFamilyIdentifiersDto::class,
    resultSetMapping = FIND_FAMILY_IDENTIFIERS_QUERY
)
class RelayDetails(
    @Column(length = 7, columnDefinition = "bpchar(7)")
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

    @Column(length = 40, columnDefinition = "bpchar(40)")
    var fingerprint: String,

    var isHibernating: Boolean,

    var uptime: Long?,

    var contact: String?,

    @Column(columnDefinition = "text")
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
