package com.torusage.database.entity

import com.torusage.jointToCommaSeparated
import org.torproject.descriptor.NetworkStatusEntry
import java.util.*
import javax.persistence.*

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Suppress("unused")
@Entity
@Table(
    indexes = [
        Index(columnList = "month, fingerprints", unique = true),
    ]
)
class NodeFamily(
    sortedFingerprints: SortedSet<String>,
    var month: String,

    @Id
    @GeneratedValue
    val id: Long? = null,
) {
    @Lob
    val fingerprints: String = sortedFingerprints.jointToCommaSeparated()
}
