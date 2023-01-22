package org.tormap.database.repository

import org.springframework.data.repository.ListCrudRepository
import org.tormap.database.entity.RelayDetails

interface RelayDetailsRepository : ListCrudRepository<RelayDetails, Long> {
    fun findByMonthAndFingerprint(month: String, fingerprint: String): RelayDetails?
    fun findAllByMonthAndFamilyEntriesNotNull(month: String): List<RelayDetails>
    fun findAllByMonthAndAutonomousSystemNumberNull(month: String): List<RelayDetails>
    fun findAllByFamilyId(familyId: Long): List<RelayDetails>
}
