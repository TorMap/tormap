package com.torusage.database.repository.archive

import org.springframework.data.jpa.repository.Query
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface ProcessedDescriptorRepositoryImpl : ProcessedDescriptorRepository {
    @Query("SELECT DISTINCT id.day FROM ProcessedDescriptor WHERE id.type = 0 ORDER BY id.day")
    fun findDistinctDays(): List<LocalDate>
}
