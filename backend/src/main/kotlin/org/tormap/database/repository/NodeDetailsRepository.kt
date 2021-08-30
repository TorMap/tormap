package org.tormap.database.repository

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.NodeDetails

interface NodeDetailsRepository : CrudRepository<NodeDetails, Long> {
    fun findByMonthAndFingerprint(month: String, fingerprint: String): NodeDetails?
    fun findAllByMonthEqualsAndFamilyEntriesNotNull(month: String): List<NodeDetails>
    fun findAllByMonthEqualsAndAutonomousSystemNumberNull(month: String): List<NodeDetails>
    fun findAllByFamilyId(familyId: Long): List<NodeDetails>
}
