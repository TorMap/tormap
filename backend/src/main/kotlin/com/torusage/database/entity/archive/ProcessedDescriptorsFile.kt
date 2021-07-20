package com.torusage.database.entity.archive

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Id

/**
 * This entity is used to record which descriptors files have been processed
 */
@Suppress("unused")
@Entity
class ProcessedDescriptorsFile(
    @Id
    var filename: String,
    var lastModified: Long,
    var processedAt: LocalDateTime = LocalDateTime.now()
)
