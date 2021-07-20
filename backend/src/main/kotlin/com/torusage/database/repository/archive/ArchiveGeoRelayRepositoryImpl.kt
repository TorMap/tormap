package com.torusage.database.repository.archive

import org.springframework.data.jpa.repository.Query
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface ArchiveGeoRelayRepositoryImpl : ArchiveGeoRelayRepository {
    @Query("SELECT DISTINCT day FROM ArchiveGeoRelay ORDER BY day")
    fun findDistinctDays(): List<LocalDate>
}
