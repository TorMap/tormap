@file:Suppress("FunctionName")

package org.tormap.database.repository

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.tormap.database.entity.DescriptorFileId
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.ProcessedFile

/**
 * Repository to interact with DB
 */
interface ProcessedFileRepository : JpaRepository<ProcessedFile, DescriptorFileId> {
    fun findAllById_TypeEqualsAndErrorNull(descriptorType: DescriptorType): List<ProcessedFile>

    @Transactional
    @Modifying
    fun deleteAllById_TypeEqualsAndLastModifiedBefore(descriptorType: DescriptorType, lastModifiedBefore: Long)
}
