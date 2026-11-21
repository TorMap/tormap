package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.tormap.database.entity.RelayDetails

interface RelayDetailsRepository : JpaRepository<RelayDetails, Long> {
    fun findByMonthAndFingerprint(month: String, fingerprint: String): RelayDetails?

    /**
     * Relays of the [month] that declare a family, by a family line or by family certificates
     */
    @Query("FROM RelayDetails WHERE month = :month AND (familyEntries IS NOT NULL OR familyIds IS NOT NULL)")
    fun findAllWithFamilyDeclarationByMonth(@Param("month") month: String): List<RelayDetails>

    fun findAllByMonthEqualsAndAutonomousSystemNumberNull(month: String): List<RelayDetails>
    fun findAllByFamilyId(familyId: Long): List<RelayDetails>
    fun findAllByIdIn(ids: List<Long>): List<RelayDetails>
}
