package org.tormap.database.entity

import java.io.Serializable
import java.time.LocalDateTime
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
    @Enumerated
    var type: DescriptorType,

    var filename: String,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DescriptorsFileId) return false

        if (type != other.type) return false
        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + filename.hashCode()
        return result
    }
}

enum class DescriptorType {
    RELAY_CONSENSUS,
    SERVER,
}
