@file:Suppress("FunctionName")

package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.GeoRelay
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface GeoRelayRepository : JpaRepository<GeoRelay, Long> {
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean
}
