@file:Suppress("FunctionName")

package org.tormap.database.repository

import org.springframework.data.repository.ListCrudRepository
import org.tormap.database.entity.RelayLocation
import java.time.LocalDate

/**
 * Repository to interact with DB
 */
interface RelayLocationRepository : ListCrudRepository<RelayLocation, Long> {
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean
}
