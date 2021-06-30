package com.torusage.database.entity

import javax.persistence.Entity
import javax.persistence.Id

/**
 * This entity is used to record which descriptors have been processed
 */
@Suppress("unused")
@Entity
class DescriptorFile(
    @Id
    var filename: String,
    var time: Long,
)
