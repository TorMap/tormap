package com.torusage.database.entity

import org.torproject.descriptor.NetworkStatusEntry
import java.text.SimpleDateFormat
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.EmbeddedId
import javax.persistence.Entity

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Suppress("unused")
@Entity
class GeoNode(
    networkStatusEntry: NetworkStatusEntry,
    seen: Date,
    var longitude: Double,
    var latitude: Double,
) {
    @EmbeddedId
    var id: GeoNodeId = GeoNodeId(
        networkStatusEntry.fingerprint,
        SimpleDateFormat("yyyy-MM").format(seen)
    )

    @ElementCollection
    var flags: List<String> = networkStatusEntry.flags.toList()
}
