package com.torusage.database.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface GeoRelayRepositoryImpl : GeoRelayRepository {
    @Query("SELECT DISTINCT day FROM GeoRelay ORDER BY day")
    fun findDistinctDays(): List<LocalDate>

    @Transactional
    @Modifying
    @Query(
        "UPDATE GeoRelay g " +
                "set g.nodeDetailsId = (SELECT d.id FROM NodeDetails d WHERE function('FORMATDATETIME', g.day, 'yyyy-MM') = d.month AND g.fingerprint = d.fingerprint)," +
                "g.nodeFamilyId = (SELECT d.familyId FROM NodeDetails d WHERE function('FORMATDATETIME', g.day, 'yyyy-MM') = d.month AND g.fingerprint = d.fingerprint)" +
                "where exists (SELECT d.id FROM NodeDetails d WHERE function('FORMATDATETIME', g.day, 'yyyy-MM') = d.month AND g.fingerprint = d.fingerprint)"
    )
    fun updateForeignIds(): Int
}
