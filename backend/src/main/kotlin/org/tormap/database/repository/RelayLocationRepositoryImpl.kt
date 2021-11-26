package org.tormap.database.repository

import org.springframework.data.jpa.repository.Query
import org.tormap.adapter.dto.RelayLocationDto
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface RelayLocationRepositoryImpl : RelayLocationRepository {
    @Query("SELECT DISTINCT day FROM RelayLocation ORDER BY day")
    fun findDistinctDays(): Set<LocalDate>

    @Query(
        "SELECT new org.tormap.adapter.dto.RelayLocationDto(g.latitude, g.longitude, g.countryCode, g.flags, n.id, n.familyId) FROM RelayLocation g " +
                "LEFT JOIN FETCH RelayDetails n " +
                "ON g.fingerprint = n.fingerprint " +
                "AND function('FORMATDATETIME', g.day, 'yyyy-MM') = n.month " +
                "WHERE g.day = :day"
    )
    fun findAllUsingDay(day: LocalDate): List<RelayLocationDto>
}
