package com.torusage.database.repository.archive

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


/**
 * Repository to interact with DB
 */
interface ArchiveGeoRelayRepositoryImpl : ArchiveGeoRelayRepository {
    @Query("SELECT DISTINCT day FROM ArchiveGeoRelay ORDER BY day")
    fun findDistinctDays(): List<LocalDate>

    @Transactional
    @Modifying
    @Query(
        "UPDATE ArchiveGeoRelay g " +
                "set g.detailsId = (SELECT d.id FROM ArchiveNodeDetails d WHERE function('FORMATDATETIME', g.day, 'yyyy-MM') = d.month AND g.fingerprint = d.fingerprint)"
    )
    fun updateDetailsIds(): Int

    @Transactional
    @Modifying
    @Query(
        "UPDATE ArchiveGeoRelay g " +
                "set g.familyId = (SELECT f.id FROM ArchiveNodeFamily f WHERE function('FORMATDATETIME', g.day, 'yyyy-MM') = f.month AND f.fingerprints LIKE concat('%', g.fingerprint,'%'))"
    )
    fun updateFamilyIds(): Int
}
