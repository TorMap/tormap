package org.tormap.database.repository

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.DescriptorsFile


/**
 * Repository to interact with DB
 */
interface DescriptorsFileRepository : CrudRepository<DescriptorsFile, String>
