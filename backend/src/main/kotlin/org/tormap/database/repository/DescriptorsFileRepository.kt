package org.tormap.database.repository

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.DescriptorType
import org.tormap.database.entity.DescriptorsFile
import org.tormap.database.entity.DescriptorsFileId


/**
 * Repository to interact with DB
 */
interface DescriptorsFileRepository : CrudRepository<DescriptorsFile, DescriptorsFileId> {
    fun findAllById_TypeEqualsAndErrorsNull(descriptorType: DescriptorType): List<DescriptorsFile>
}
