package com.torusage.database.entity.archive

import org.torproject.descriptor.NetworkStatusEntry
import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.Enumerated
import javax.persistence.Temporal
import javax.persistence.TemporalType

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Embeddable
class DescriptorId(
    @Enumerated
    var type: DescriptorType,

    @Temporal(TemporalType.DATE)
    var day: Calendar,
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is DescriptorId && this.type == other.type && this.day == other.day

    override fun hashCode(): Int =
        Objects.hash(this.type, this.day)
}

enum class DescriptorType {
    CONSENSUS,
    SERVER,
}
