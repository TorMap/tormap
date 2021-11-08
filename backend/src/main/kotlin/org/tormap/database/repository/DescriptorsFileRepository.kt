@file:Suppress("FunctionName")

package org.tormap.database.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.DescriptorsFile
import org.tormap.database.entity.DescriptorsFileId
import javax.transaction.Transactional


/**
 * Repository to interact with DB
 */
interface DescriptorsFileRepository : CrudRepository<DescriptorsFile, DescriptorsFileId> {
    fun findAllById_TypeEqualsAndErrorNull(descriptorType: DescriptorType): List<DescriptorsFile>

    @Transactional
    @Modifying
    fun deleteAllById_TypeEqualsAndLastModifiedAfter(descriptorType: DescriptorType, lastModifiedAfter: Long)
}
