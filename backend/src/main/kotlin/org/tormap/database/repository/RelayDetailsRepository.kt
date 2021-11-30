package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.RelayDetails

interface RelayDetailsRepository : JpaRepository<RelayDetails, Long> {
    fun findByMonthAndFingerprint(month: String, fingerprint: String): RelayDetails?
    fun findAllByMonthEqualsAndFamilyEntriesNotNull(month: String): List<RelayDetails>
    fun findAllByMonthEqualsAndAutonomousSystemNumberNull(month: String): List<RelayDetails>
    fun findAllByFamilyId(familyId: Long): List<RelayDetails>
}
