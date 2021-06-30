package com.torusage.database.repository

import com.torusage.database.entity.DescriptorFile
import org.springframework.data.repository.CrudRepository
import java.util.*


/**
 * Repository to interact with DB
 */
interface DescriptorFileRepository : CrudRepository<DescriptorFile, String>
