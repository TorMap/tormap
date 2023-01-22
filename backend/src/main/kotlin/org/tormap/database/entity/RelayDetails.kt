package org.tormap.database.entity

import org.springframework.data.relational.core.mapping.Table
import org.tormap.adapter.dto.RelayFamilyIdentifiersDto
import org.tormap.util.jointToCommaSeparated
import org.tormap.util.stripLengthForDB
import org.torproject.descriptor.ServerDescriptor
import java.time.LocalDate

const val FIND_FAMILY_IDENTIFIERS_QUERY = "findFamilyIdentifiers"

/**
 * This entity is used to store details from a relay or bridge [ServerDescriptor].
 * Documentation about attributes can be found at [https://metrics.torproject.org/metrics-lib/org/torproject/descriptor/ServerDescriptor.html].
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
@Table("relay_details")
@SqlResultSetMapping(
    name = FIND_FAMILY_IDENTIFIERS_QUERY,
    classes = [
        ConstructorResult(
            targetClass = RelayFamilyIdentifiersDto::class,
            columns = [
                ColumnResult(name = "id", type = Long::class),
                ColumnResult(name = "memberCount", type = Long::class),
                ColumnResult(name = "nicknames", type = String::class),
                ColumnResult(name = "autonomousSystems", type = String::class)
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
    var fingerprint: String,
    var isHibernating: Boolean,
    var uptime: Long?,
    var contact: String?,
    var familyEntries: String?,
    var familyId: Long?,
    var cachesExtraInfo: Boolean,
    var isHiddenServiceDir: Boolean,
    var linkProtocolVersions: String?,
    var circuitProtocolVersions: String?,
    var tunnelledDirServer: Boolean
) : AbstractBaseEntity<Long>() {

    constructor(
        descriptor: ServerDescriptor,
        month: String,
        day: LocalDate,
        autonomousSystemName: String?,
        autonomousSystemNumber: Int?,
        id: Long?
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
        protocols = descriptor.protocols?.map { "${it.key} (${it.value.jointToCommaSeparated()})" }?.jointToCommaSeparated().stripLengthForDB(),
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
