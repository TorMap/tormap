package com.torusage.database.entity.archive

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

/**
 * This entity is used to record which descriptors have been processed
 */
@Suppress("unused")
@Entity
class ProcessedDescriptorsFile(
    @Id
    var filename: String,
    var lastModified: Long,
    var processedAt: Date = Date()
)
