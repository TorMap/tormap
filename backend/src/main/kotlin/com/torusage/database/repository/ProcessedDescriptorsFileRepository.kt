package com.torusage.database.repository

import com.torusage.database.entity.ProcessedDescriptorsFile
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB
 */
interface ProcessedDescriptorsFileRepository : CrudRepository<ProcessedDescriptorsFile, String>
