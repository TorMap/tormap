package com.torusage.database.repository

import com.torusage.database.entity.DescriptorsFile
import org.springframework.data.repository.CrudRepository


/**
 * Repository to interact with DB
 */
interface DescriptorsFileRepository : CrudRepository<DescriptorsFile, String>
