package com.torusage.database.entity.archive

import java.util.*
import javax.persistence.EmbeddedId
import javax.persistence.Entity

/**
 * This entity is used to record which descriptors have been processed
 */
@Suppress("unused")
@Entity
class ProcessedDescriptor(
    @EmbeddedId
    var id: DescriptorId,

    var processedAt: Date = Date(),
)
