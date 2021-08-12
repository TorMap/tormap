@file:Suppress("FunctionName")

package org.tormap.database.repository

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.GeoRelay
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface GeoRelayRepository : CrudRepository<GeoRelay, Long> {
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean
}
