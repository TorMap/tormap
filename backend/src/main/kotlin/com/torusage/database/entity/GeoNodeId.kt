package com.torusage.database.entity

import org.torproject.descriptor.NetworkStatusEntry
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Embeddable
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

/**
 * This entity is used to store relevant information about a [NetworkStatusEntry]
 */
@Embeddable
class GeoNodeId(
    @Column(length = 40)
    var fingerprint: String,

    @Column(length = 7)
    var seenInMonth: String,
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is GeoNodeId && this.fingerprint == other.fingerprint && this.seenInMonth == other.seenInMonth

    override fun hashCode(): Int =
        Objects.hash(this.fingerprint, this.seenInMonth)
}
