package org.tormap.database.repository

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.ListCrudRepository
import org.tormap.database.entity.RelayLocation
import java.time.LocalDate

/**
 * Repository to interact with DB
 */
interface RelayLocationRepository : ListCrudRepository<RelayLocation, Long> {
    @Query("SELECT exists(SELECT 1 FROM relay_location WHERE day = :day AND fingerprint = :fingerprint)")
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean
}
