package org.tormap.database.entity

import org.tormap.stripLengthForDB
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.*

/**
 * This entity is used to record which descriptors files have been processed
 */
@Suppress("unused")
@Entity
class DescriptorsFile(
    @EmbeddedId
    var id: DescriptorsFileId,
    var lastModified: Long,
    errors: String? = null,
    var processedAt: LocalDateTime = LocalDateTime.now(),
) {
    var error: String? = errors.stripLengthForDB()
        set (value) { field = value.stripLengthForDB() }
}

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
