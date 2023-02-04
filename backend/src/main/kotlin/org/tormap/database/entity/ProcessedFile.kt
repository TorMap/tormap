package org.tormap.database.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import org.tormap.util.stripLengthForDB
import java.time.LocalDateTime

/**
 * This entity is used to record which descriptors files have been processed
 */
@Table("processed_file")
@Suppress("unused")
class ProcessedFile @PersistenceCreator constructor(
    @Id val filename: String,
    private var type: DescriptorType,
    var lastModified: Long,
    var processedAt: LocalDateTime = LocalDateTime.now(),
    error: String? = null
) : Persistable<String> {

    @Transient
    private var isNew = false

    var error: String? = error
        set(value) {
            field = value.stripLengthForDB()
        }

    override fun getId(): String = filename

    override fun isNew(): Boolean = isNew

    fun setNew() {
        isNew = true
    }
}

enum class DescriptorType {
    ARCHIVE_RELAY_CONSENSUS,
    ARCHIVE_RELAY_SERVER,
    RECENT_RELAY_CONSENSUS,
    RECENT_RELAY_SERVER;

    fun isRecent() = this === RECENT_RELAY_CONSENSUS || this === RECENT_RELAY_SERVER
}
