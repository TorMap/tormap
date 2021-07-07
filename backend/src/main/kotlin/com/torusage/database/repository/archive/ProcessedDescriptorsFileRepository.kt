package com.torusage.database.repository.archive

import com.torusage.database.entity.archive.ProcessedDescriptorsFile
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB
 */
interface ProcessedDescriptorsFileRepository : CrudRepository<ProcessedDescriptorsFile, String>
