package com.torusage.database.entity.archive

import java.time.LocalDateTime
import javax.persistence.EmbeddedId
import javax.persistence.Entity

/**
 * This entity is used to record which descriptors files have been processed
 */
@Suppress("unused")
@Entity
class DescriptorsFile(
    @EmbeddedId
    var id: DescriptorFileId,
    var lastModified: Long,
    var processedAt: LocalDateTime = LocalDateTime.now()
)

enum class DescriptorType {
    RELAY_CONSENSUS,
    SERVER,
}
