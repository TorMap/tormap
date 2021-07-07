package com.torusage.database.entity.archive

import org.torproject.descriptor.NetworkStatusEntry
import java.text.SimpleDateFormat
import java.util.*
import javax.persistence.EmbeddedId
import javax.persistence.Entity

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Suppress("unused")
@Entity
class ArchiveGeoRelay(
    networkStatusEntry: NetworkStatusEntry,
    seenAt: Date,
    var latitude: Double,
    var longitude: Double,
) {
    @EmbeddedId
    var id: ArchiveNodeId = ArchiveNodeId(
        networkStatusEntry.fingerprint,
        SimpleDateFormat("yyyy-MM").format(seenAt)
    )

    var flags: String? = networkStatusEntry.flags.joinToString(", ")
}
