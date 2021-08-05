package org.tormap.database.repository

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.NodeDetails

interface NodeDetailsRepository : CrudRepository<NodeDetails, Long> {
    fun getByMonthAndFingerprint(month: String, fingerprint: String): NodeDetails?
    fun getAllByMonthAndNickname(month: String, nickname: String): List<NodeDetails>
    fun getAllByMonthInAndFamilyEntriesNotNullAndFamilyIdNotNull(month: Set<String>): List<NodeDetails>
    fun getAllByFamilyEntriesNotNullAndFamilyIdNotNull(): List<NodeDetails>
}
