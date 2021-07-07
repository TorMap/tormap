package com.torusage.database.repository.archive

import org.springframework.data.jpa.repository.Query


/**
 * Repository to interact with DB
 */
interface ArchiveGeoRelayRepositoryImpl : ArchiveGeoRelayRepository {
    @Query("SELECT DISTINCT id.seenInMonth FROM ArchiveGeoRelay ORDER BY id.seenInMonth")
    fun findDistinctMonths(): List<String>
}
