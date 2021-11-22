@file:Suppress("FunctionName")

package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.DescriptorsFile
import org.tormap.database.entity.DescriptorsFileId
import javax.transaction.Transactional


/**
 * Repository to interact with DB
 */
interface DescriptorsFileRepository : JpaRepository<DescriptorsFile, DescriptorsFileId> {
    fun findAllById_TypeEqualsAndErrorNull(descriptorType: DescriptorType): List<DescriptorsFile>

    @Transactional
    @Modifying
    fun deleteAllById_TypeEqualsAndLastModifiedBefore(descriptorType: DescriptorType, lastModifiedBefore: Long)
}
