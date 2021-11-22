package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.NodeDetails

interface NodeDetailsRepository : JpaRepository<NodeDetails, Long> {
    fun findByMonthAndFingerprint(month: String, fingerprint: String): NodeDetails?
    fun findAllByMonthEqualsAndFamilyEntriesNotNull(month: String): List<NodeDetails>
    fun findAllByMonthEqualsAndAutonomousSystemNumberNull(month: String): List<NodeDetails>
    fun findAllByFamilyId(familyId: Long): List<NodeDetails>
}
