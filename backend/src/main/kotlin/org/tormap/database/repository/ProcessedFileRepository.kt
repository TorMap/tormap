package org.tormap.database.repository

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.stereotype.Repository
import org.tormap.database.entity.DescriptorFileId
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.ProcessedFile

/**
 * Repository to interact with DB
 */
@Repository
interface ProcessedFileRepository : ListCrudRepository<ProcessedFile, DescriptorFileId> {
    @Query(
        """SELECT type, filename, last_modified, processed_at, error
           FROM processed_file
           WHERE type = :descriptorType AND error IS NULL"""
    )
    fun findAllByTypeAndErrorNull(descriptorType: DescriptorType): List<ProcessedFile>

    @Modifying
    @Query("DELETE FROM processed_file WHERE type = :descriptorType AND last_modified < :lastModified")
    fun deleteAllByTypeAndLastModifiedBefore(descriptorType: DescriptorType, lastModified: Long)
}
