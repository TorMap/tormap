package com.torusage.database.entity.archive

import org.torproject.descriptor.NetworkStatusEntry
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.*

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Suppress("unused")
@Entity
@Table(
    indexes = [
        Index(columnList = "fingerprint, day", unique = true)
    ]
)
class ArchiveGeoRelay(
    networkStatusEntry: NetworkStatusEntry,
    var day: LocalDate,
    var latitude: BigDecimal,
    var longitude: BigDecimal,
) {
    @Id
    @GeneratedValue
    val id: Long? = null

    @Column(length = 40)
    var fingerprint: String = networkStatusEntry.fingerprint

    var flags: String? = networkStatusEntry.flags.joinToString(", ")
}
