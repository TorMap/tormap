package com.torusage.database.repository.archive

import com.torusage.database.entity.archive.DescriptorId
import com.torusage.database.entity.archive.ProcessedDescriptor
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB
 */
interface ProcessedDescriptorRepository : CrudRepository<ProcessedDescriptor, DescriptorId>
