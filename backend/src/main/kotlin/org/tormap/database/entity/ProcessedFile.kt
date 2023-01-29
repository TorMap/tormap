package org.tormap.database.entity

import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.relational.core.mapping.Table
import org.tormap.util.stripLengthForDB
import java.time.LocalDateTime

/**
 * This entity is used to record which descriptors files have been processed
 */
@Table("processed_file")
@Suppress("unused")
class ProcessedFile @PersistenceCreator private constructor(
    private var type: DescriptorType,
    private var filename: String,
    var lastModified: Long,
    var processedAt: LocalDateTime = LocalDateTime.now(),
    error: String? = null,
) {
    constructor(
        id: DescriptorFileId,
        lastModified: Long,
        processedAt: LocalDateTime = LocalDateTime.now(),
        error: String? = null
    ) : this(id.type, id.filename, lastModified, processedAt, error)

    var id: DescriptorFileId
        get() = DescriptorFileId(type, filename)
        set(id) {
            type = id.type
            filename = id.filename
        }

    var error: String? = error
        set(value) {
            field = value.stripLengthForDB()
        }
}

/**
 * This represents a composite id
 */
class DescriptorFileId(val type: DescriptorType, val filename: String)

enum class DescriptorType {
    ARCHIVE_RELAY_CONSENSUS,
    ARCHIVE_RELAY_SERVER,
    RECENT_RELAY_CONSENSUS,
    RECENT_RELAY_SERVER;

    fun isRecent() = this === RECENT_RELAY_CONSENSUS || this === RECENT_RELAY_SERVER
}
