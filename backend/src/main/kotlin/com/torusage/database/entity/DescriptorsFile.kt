package com.torusage.database.entity

import java.io.Serializable
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Enumerated

/**
 * This entity is used to record which descriptors files have been processed
 */
@Suppress("unused")
@Entity
class DescriptorsFile(
    @EmbeddedId
    var id: DescriptorsFileId,
    var lastModified: Long,
    var processedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * This represents a composite id
 */
@Embeddable
class DescriptorsFileId(
    var filename: String,

    @Enumerated
    var type: DescriptorType,
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is DescriptorsFileId && Objects.equals(other, this)

    override fun hashCode(): Int =
        Objects.hash(this.filename, this.type)
}

enum class DescriptorType {
    RELAY_CONSENSUS,
    SERVER,
}
