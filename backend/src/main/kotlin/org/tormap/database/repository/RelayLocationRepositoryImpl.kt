package org.tormap.database.repository

import org.springframework.data.jdbc.repository.query.Query
import org.tormap.adapter.dto.RelayLocationDto
import java.time.LocalDate

/**
 * Repository to interact with DB
 */
interface RelayLocationRepositoryImpl : RelayLocationRepository {
    @Query("SELECT DISTINCT day FROM relay_location")
    fun findDistinctDays(): Set<LocalDate>

    @Query(
        """SELECT g.latitude AS lat,
                  g.longitude AS long,
                  g.country_code AS country,
                  trim('{' FROM trim('}' FROM g.flags_numeric)) AS flags,
                  n.id AS detailsId,
                  n.family_id AS familyId
           FROM relay_location g
           LEFT JOIN relay_details n ON g.fingerprint = n.fingerprint AND to_char(g.day, 'YYYY-MM') = n.month
           WHERE g.day = :day"""
    )
    fun findAllUsingDay(day: LocalDate): List<RelayLocationDto>
}
