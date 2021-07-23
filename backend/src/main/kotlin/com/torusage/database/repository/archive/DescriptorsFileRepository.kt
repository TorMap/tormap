package com.torusage.database.repository.archive

import com.torusage.database.entity.archive.DescriptorsFile
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB
 */
interface DescriptorsFileRepository : CrudRepository<DescriptorsFile, String>
