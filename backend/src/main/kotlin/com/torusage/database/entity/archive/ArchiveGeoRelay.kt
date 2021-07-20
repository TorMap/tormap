package com.torusage.database.entity.archive

import com.torusage.adapter.controller.model.RelayFlag
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
        Index(columnList = "fingerprint, day", unique = true),
        Index(columnList = "day"),
    ]
)
class ArchiveGeoRelay(
    networkStatusEntry: NetworkStatusEntry,
    var day: LocalDate,
    var latitude: BigDecimal,
    var longitude: BigDecimal,
    var countryIsoCode: String?,
) {
    @Id
    @GeneratedValue
    val id: Long? = null

    @Column(length = 40)
    var fingerprint: String = networkStatusEntry.fingerprint

    var flags: String? = try {
        networkStatusEntry.flags.map { RelayFlag.valueOf(it.toString()).ordinal }.joinToString(", ")
    } catch (exception: Exception) {
        null
    }
}
