package com.torusage.database.repository;

import com.torusage.database.entity.NodeDetails
import org.springframework.data.repository.CrudRepository

interface NodeDetailsRepository : CrudRepository<NodeDetails, Long> {
    fun getByMonthAndFingerprint(month: String, fingerprint: String): NodeDetails?
    fun getAllByMonthAndNickname(month: String, nickname: String): List<NodeDetails>
    fun getAllByMonthInAndFamilyEntriesNotNull(month: Set<String>): List<NodeDetails>
}
