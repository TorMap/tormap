package com.torusage.database.repository

import org.springframework.data.jpa.repository.Query


/**
 * Repository to interact with DB
 */
interface GeoNodeRepositoryImpl : GeoNodeRepository {
    @Query("SELECT DISTINCT id.seenInMonth FROM GeoNode ORDER BY id.seenInMonth")
    fun findDistinctMonths(): List<String>
}
