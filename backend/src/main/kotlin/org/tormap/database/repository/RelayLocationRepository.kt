@file:Suppress("FunctionName")

package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.RelayLocation
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface RelayLocationRepository : JpaRepository<RelayLocation, Long> {
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean
}
