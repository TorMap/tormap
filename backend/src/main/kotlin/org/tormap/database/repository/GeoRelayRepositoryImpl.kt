package org.tormap.database.repository

import org.springframework.data.jpa.repository.Query
import org.tormap.adapter.controller.view.GeoRelayView
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface GeoRelayRepositoryImpl : GeoRelayRepository {
    @Query("SELECT DISTINCT day FROM GeoRelay ORDER BY day")
    fun findDistinctDays(): List<LocalDate>

    @Query(
        "SELECT new org.tormap.adapter.controller.view.GeoRelayView(g.latitude, g.longitude, g.countryIsoCode, g.flags, n.id, n.familyId) FROM GeoRelay g " +
                "LEFT JOIN FETCH NodeDetails n " +
                "ON g.fingerprint = n.fingerprint " +
                "AND function('FORMATDATETIME', g.day, 'yyyy-MM') = n.month " +
                "WHERE g.day = :day"
    )
    fun findAllUsingDay(day: LocalDate): List<GeoRelayView>
}
