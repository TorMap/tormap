@file:Suppress("FunctionName")

package com.torusage.database.repository

import com.torusage.database.entity.GeoRelay
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface GeoRelayRepository : CrudRepository<GeoRelay, Long> {
    fun findAllByDay(day: LocalDate): List<GeoRelay>
    fun existsByDayAndFingerprint(day: LocalDate, fingerprint: String): Boolean
}
