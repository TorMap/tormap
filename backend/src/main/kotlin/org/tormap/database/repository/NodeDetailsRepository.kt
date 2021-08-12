package org.tormap.database.repository

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.NodeDetails

interface NodeDetailsRepository : CrudRepository<NodeDetails, Long> {
    fun findByMonthAndFingerprint(month: String, fingerprint: String): NodeDetails?
    fun findByMonthAndFingerprintAndFamilyEntriesNotNull(month: String, fingerprint: String): NodeDetails?
    fun getAllByMonthAndNickname(month: String, nickname: String): List<NodeDetails>
    fun getAllByMonthEqualsAndFamilyEntriesNotNull(month: String): List<NodeDetails>
    fun findAllByFamilyId(familyId: Long): List<NodeDetails>
}
