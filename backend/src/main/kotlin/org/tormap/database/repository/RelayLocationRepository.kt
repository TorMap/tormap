package org.tormap.database.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.springframework.stereotype.Repository
import org.tormap.adapter.dto.RelayLocationDto
import org.tormap.database.entity.RelayLocation
import java.time.LocalDate

/**
 * Repository to interact with DB
 */
@Repository
interface RelayLocationRepository : ListCrudRepository<RelayLocation, Long> {
    @Query("SELECT exists(SELECT 1 FROM relay_location WHERE day = :day AND fingerprint = :fingerprint)")
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean

    @Query("SELECT DISTINCT day FROM relay_location")
    fun findDistinctDays(): List<LocalDate>

    @Query(
        """SELECT location.latitude AS lat,
                  location.longitude AS long,
                  location.country_code AS country,
                  trim('{' FROM trim('}' FROM location.flags_numeric)) AS flags,
                  details.id AS details_id,
                  details.family_id AS family_id
           FROM relay_location location
           LEFT JOIN relay_details details ON location.fingerprint = details.fingerprint AND to_char(location.day, 'YYYY-MM') = details.month
           WHERE location.day = :day"""
    )
    fun findAllUsingDay(day: LocalDate): List<RelayLocationDto>
}
