package com.torusage.database.entity.archive

import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.Enumerated

/**
 * This is composite id for entities
 */
@Embeddable
class DescriptorFileId(
    var filename: String,

    @Enumerated
    var type: DescriptorType,
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is DescriptorFileId && Objects.equals(this, other)

    override fun hashCode(): Int =
        Objects.hash(this.filename, this.type)
}
